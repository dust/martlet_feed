package com.kmfrog.martlet.feed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.Side;
import com.kmfrog.martlet.feed.impl.BinanceInstrumentDepth;
import com.kmfrog.martlet.feed.impl.BinanceWebSocketHandler;
import com.kmfrog.martlet.feed.impl.HuobiInstrumentDepth;
import com.kmfrog.martlet.feed.impl.HuobiWebSocketHandler;

import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;

/**
 * Hello world!
 *
 */
public class App implements ResetController {
    /**
     * 所有交易对的聚合订单表。
     */
    private final Long2ObjectArrayMap<AggregateOrderBook> books;
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static ExecutorService executor = Executors
            .newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("WebSocketRunnable-%d").build());
    private final Map<Source, WebSocketDaemon> websocketDaemons;

    public App() {
        books = new Long2ObjectArrayMap<>();
        websocketDaemons = new ConcurrentHashMap<>();
    }

    AggregateOrderBook makesureOrderBook(long instrument) {
        return books.computeIfAbsent(instrument, (key) -> new AggregateOrderBook(key));
    }

    void startSnapshotTask(String symbol, SnapshotDataListener listener) {
        Runnable r = new RestSnapshotRunnable(
                String.format("https://www.binance.com/api/v1/depth?symbol=%s&limit=10", symbol), "GET", null, null,
                listener);
        executor.submit(r);
    }

    void startWebSocket(Source source, BaseWebSocketHandler handler) {
        WebSocketDaemon wsDaemon = new WebSocketDaemon(handler);
        websocketDaemons.put(source, wsDaemon);
        wsDaemon.keepAlive();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // IDataListener listener = new IDataListener() {
        //
        // @Override
        // public Object onMessage(String msg) {
        // System.out.println(msg);
        // return null;
        // }
        //
        // };

        // Binance bin = new Binance("https://www.binance.com/api/v1/depth?symbol=%s&limit=1000",
        // "wss://stream.binance.com:9443/ws/bnbbtc@depth", "BNBBTC");
        App app = new App();
        Instrument bnbbtc = new Instrument("BTCUSDT", 8, 8);
//        Instrument bnbeth = new Instrument("BNBETH", 8, 8);
        AggregateOrderBook btcBook = app.makesureOrderBook(bnbbtc.asLong());
//        AggregateOrderBook bnbethBook = app.makesureOrderBook(bnbeth.asLong());
//
        BinanceInstrumentDepth btc = new BinanceInstrumentDepth(bnbbtc, btcBook, Source.Binance, app);
//        BinanceInstrumentDepth eth = new BinanceInstrumentDepth(bnbeth, bnbethBook, Source.Binance, app);
        app.startSnapshotTask("BTCUSDT", btc);
//        app.startSnapshotTask("BNBETH", eth);
        BaseWebSocketHandler handler = new BinanceWebSocketHandler(
                "wss://stream.binance.com:9443/stream?streams=%s@depth", new String[] { "btcusdt"},
                new BinanceInstrumentDepth[] { btc });
        app.startWebSocket(Source.Binance, handler);
        
        Instrument btcusdt = new Instrument("BTCUSDT", 8, 8);
//        AggregateOrderBook btcBook = app.makesureOrderBook(btcusdt.asLong());
//        
        HuobiInstrumentDepth hbBtc = new HuobiInstrumentDepth(btcusdt, btcBook, Source.Huobi, app);
        BaseWebSocketHandler hbHandler = new HuobiWebSocketHandler(new String[] {"btcusdt"}, new HuobiInstrumentDepth[] {
                hbBtc 
        });
        app.startWebSocket(Source.Huobi, hbHandler);

        while (true) {
            Thread.sleep(10000L);
            btcBook.dump(Side.BUY, System.out);
            System.out.println("\n#####\n");
//            bnbethBook.dump(Side.BUY, System.out);
//            System.out.println("\n=====\n");
//            btcBook.dump(Side.BUY, System.out);
//            System.out.println("\n====\n");
            
            
        }
    }

    public void reset(Source mkt, Instrument instrument, BinanceInstrumentDepth depth, boolean isRest, boolean isWs) {
        this.startSnapshotTask(instrument.asString().toUpperCase(), depth);
    }

}
