package com.kmfrog.martlet.feed;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.common.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * websocket实例，它能被轮询保持正常活跃。
 * 
 * @author dust Oct 11, 2019
 *
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebSocketDaemon implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(WebSocketDaemon.class);
    private final String wsUrlFmt;
//    private WebSocketClient client;
    private final WsDataListener listener;
    private final ReentrantLock lock;
    private final Set<String> symbolNames;

    WebSocketSession session;

    public WebSocketDaemon(String wsUrl, Set<String> symbolNames, WsDataListener listener) {
        this.wsUrlFmt = wsUrl;
        this.listener = listener;
        this.symbolNames = symbolNames;

        lock = new ReentrantLock();
//        client = new WebSocketClient();
    }

//    public WebSocketClient getWebSocketClient() {
//        return client;
//    }

    public Set<String> getSymbolNames() {
        return symbolNames;
    }

    @OnWebSocketError
    public void onError(Throwable ex) {
        logger.error(ex.getMessage(), ex);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        lock.lock();

        try {
            this.session = null;
        } finally {
            lock.unlock();
        }

        if (logger.isInfoEnabled()) {
            logger.info("连接关闭,[{}],[{}]", reason, session.getRemoteAddress());
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        lock.lock();
        try {
            this.session = (WebSocketSession) session;
        } finally {
            lock.unlock();
        }

        if (logger.isInfoEnabled()) {
            logger.info("连接创建,[{}],[{}],[{}],[{}]", String.join("/", symbolNames), symbolNames, "xxx",
                    session.getRemoteAddress());
        }
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        listener.onMessage(msg);
    }

    boolean clientOpened(WebSocketClient client) {
        if (!client.isStarted()) {
            return false;
        }
        if (session == null) {
            return false;
        }
        return session.getConnection().isOpen();
    }

    public final WebSocketSession connect(WebSocketClient client) {
        // wss://stream.binance.com:9443/stream?streams=%s@depth, bnbbtc@depth/ethbtc,
        String wsUriString = String.format(wsUrlFmt, String.join("@depth/", symbolNames)); //"wss://stream.binance.com:9443/ws/bnbbtc@depth"; //
        try {
            if (!client.isStarted()) {
                client.start();
            }
            return (WebSocketSession) client.connect(this, new URI(wsUriString), new ClientUpgradeRequest()).get();
        } catch (SocketTimeoutException e) {
            logger.error("WebSocket[{}]连接超时", wsUriString, e);
            // throw new FatalException("创建WebSocket连接失败[%s]", wsUriString, e);
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("创建WebSocket连接失败[{}]", wsUriString, e);
            // throw new FatalException("创建WebSocket连接失败[%s]", wsUriString, e);
            throw new RuntimeException(e);
        }
    }

    public void run() {
        WebSocketClient client = new WebSocketClient();
//        while (true) {
//            lock.lock();
            
            try {
                if (!clientOpened(client)) {
                    session = connect(client);
                }
            } catch (Exception ex) {
                logger.error("keepAlive [{}]", String.join("/", symbolNames), ex);
            } finally {
//                lock.unlock();
            }

//            try {
//                Thread.sleep(3000L);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//            }
//        }

    }

}
