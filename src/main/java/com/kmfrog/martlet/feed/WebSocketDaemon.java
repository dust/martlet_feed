package com.kmfrog.martlet.feed;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * websocket实例，它能被轮询保持正常活跃。
 * 
 * @author dust Oct 11, 2019
 *
 */
@WebSocket(maxTextMessageSize = 64 * 1024)
public class WebSocketDaemon{

    private final BaseWebSocketHandler handler;
    private Session session;


    public WebSocketDaemon(BaseWebSocketHandler handler) {
        this.handler = handler;
        this.handler.setWebSocket(this);
    }

    public Session getWebSocketSession() {
        return session;
    }

    public Set<String> getSymbolNames() {
        return handler.getSymbols();
    }

    @OnWebSocketError
    public void onError(Throwable ex) {
        handler.onError(ex);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        handler.onClose(statusCode, reason);
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        handler.onConnect(session);
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String msg) {
        handler.onMessage(session, msg);
    }
    
    @OnWebSocketMessage
    public void onBinaryMessage(Session session, InputStream is) throws IOException {
        handler.onBinaryMessage(session, is);
    }

    public void keepAlive() {
        session = handler.keepAlive();
    }

}
