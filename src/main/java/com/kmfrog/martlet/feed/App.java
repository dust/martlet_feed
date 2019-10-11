package com.kmfrog.martlet.feed;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    void startOrderBook(final Source mkt, Set<String> symbols, BinanceInstrumentDepth depth, boolean isRest, boolean isWs) {
        if (isRest) {
            executor.submit(new RestSnapshotRunnable("https://www.binance.com/api/v1/depth?symbol=BNBBTC&limit=10", "GET",
                    null, null, depth));
        }
        if (isWs) {
            
            
                WebSocketDaemon websocket = new WebSocketDaemon("wss://stream.binance.com:9443/stream?streams=%s@depth", symbols, depth);
                
//            executor.submit(websocket);
                websocket.run();
        }
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
        Instrument bnbbtc = new Instrument("BNBBTC", 8, 8);
        Instrument bnbeth = new Instrument("BNBETH", 8, 8);
        AggregateOrderBook bnbbtcBook = app.makesureOrderBook(bnbbtc.asLong());
//        AggregateOrderBook bnbethBook = app.makesureOrderBook(bnbeth.asLong());
        Set<String> symbols = new HashSet<>();
        symbols.add("bnbbtc");
        symbols.add("bnbeth");
        BinanceInstrumentDepth btc = new BinanceInstrumentDepth(bnbbtc, bnbbtcBook, app);
//        BinanceInst
        app.startOrderBook(Source.Binance, symbols, btc, true, true);
        while (true) {
            Thread.sleep(10000L);
            bnbbtcBook.dump(Side.BUY, System.out);
            System.out.println("#####\n");
        }
    }

    public void reset(Source mkt, Instrument instrument, BinanceInstrumentDepth depth, boolean isRest, boolean isWs) {
        // Instrument instrument = new Instrument("BNBBTC", 8, 8);
        // BinanceInstrumentDepth btc = new BinanceInstrumentDepth(instrument, this);
        //// new DepthFeedRunnable("wss://stream.binance.com:9443/ws/bnbbtc@depth", listener);
        // Future f=executor.submit(new RestRunnable("https://www.binance.com/api/v1/depth?symbol=BNBBTC&limit=1000",
        // "GET", null, null, btc));
        // System.out.println(f.get());
        // f.cancel(true);
        // Thread.sleep(2000);
        // System.out.println("Done");
        Set<String> symbols = new HashSet<>();
        symbols.add("bnbbtc");
        symbols.add("ethbtc");
        startOrderBook(mkt, symbols, depth, isRest, isWs);
    }

}
