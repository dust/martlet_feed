package com.kmfrog.martlet.tac;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kmfrog.martlet.book.IOrderBook;
import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.OrderBook;
import com.kmfrog.martlet.feed.BaseInstrumentDepth;
import com.kmfrog.martlet.feed.ResetController;
import com.kmfrog.martlet.feed.Source;
import com.kmfrog.martlet.feed.WebSocketDaemon;
import com.kmfrog.martlet.feed.impl.BhexInstrumentDepth;
import com.kmfrog.martlet.feed.impl.BhexWebSocketHandler;
import com.kmfrog.martlet.tac.Main.XiePtcnThread;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import io.broker.api.client.BrokerApiClientFactory;
import io.broker.api.client.BrokerApiRestClient;

public class Main {

    static Logger logger = LoggerFactory.getLogger(InSpreadRunnable.class);

    public Main() {

    }

    public static void main(String[] args) {

        Config cfg = ConfigFactory.load();
        String baseUrl = cfg.getString("api.base.url");
        String apiKey = cfg.getString("api.key");
        String secretKey = cfg.getString("api.secret");

        // Main app = new Main();
        //
        // Instrument xie = new Instrument("XIEPTCN", 3, 3);
        //
        // BrokerApiClientFactory factory = BrokerApiClientFactory.newInstance(baseUrl, apiKey,
        // secretKey);
        // BrokerApiRestClient client = factory.newRestClient();
        // OrderBook xieBook = new OrderBook(xie.asLong());
        //
        // // NewOrderResponse resp = client.newOrder(NewOrder.limitBuy("XIEPTCN", TimeInForce.GTC, "10", "0.035"));
        // // System.out.println(resp);
        //
        // BrokerInfo info = client.getBrokerInfo();
        // try {
        // Thread.sleep(10000L);
        // } catch (InterruptedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // InSpreadRunnable r = new InSpreadRunnable(xie, xieBook, client, 3, 0);
        //// r.run();
        // BigDecimal[] vols = r.getVolume();
        // System.out.println(Arrays.asList(vols));
        //
        // List<Order> openOrders = client.getOpenOrders(new OpenOrderRequest("XIEPTCN", 100));
        // BigDecimal xieSum = new BigDecimal("0");
        // BigDecimal ptcnSum = new BigDecimal("0");
        // for(Order o : openOrders) {
        // if(o.getSide() == OrderSide.SELL) {
        // xieSum = xieSum.add(new BigDecimal(o.getOrigQty()));
        // ptcnSum = ptcnSum.add(new BigDecimal(o.getCummulativeQuoteQty()));
        // }
        // }
        //// System.out.println(openOrders);
        // System.out.format("%s|%s\n", xieSum, ptcnSum);
        //
        // Account acct = client.getAccount(BrokerConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis());
        // System.out.println(acct);
        // System.out.println(acct.getBalances());
        // System.out.println(acct.getAssetBalance("PTCN"));
        //
        // List<Trade> trades = client.getMyTrades(new MyTradeRequest());
        // System.out.println(trades);
        //
        // // MathContext mc = new MathContext(4, RoundingMode.FLOOR);
        // // BigDecimal x = BigDecimal.valueOf(1000000000000L).divide(BigDecimal.valueOf(140000), mc );
        // // Instrument i = new Instrument("X", 8, 8);
        // // System.out.println(i.getPriceDecimal(10000000, 4));
        // // System.out.println(i.getPriceStr(10000000, 4));
        // System.out.println(new DecimalFormat("0.0").format(new BigDecimal("10000")));
        // System.out.println(fmtDecimal(86931, 3));

        // Random rnd = new Random(System.currentTimeMillis());
        // for(int i=0; i<100; i++) {
        // System.out.println((long)(21 + rnd.nextDouble() * (34 - 21)));
        // }

        Instrument XIE = new Instrument("XIEPTCN", 3, 3);
        Instrument hntc = new Instrument("HNTCUSDT",4, 4);
        Instrument tac = new Instrument("TACPTCN",4, 4);
        Thread worker = new Main.XiePtcnThread(XIE, baseUrl, apiKey, secretKey, 3, 0, 2000, 19000, 5000, 59000);
        Thread hntcWorker = new Main.XiePtcnThread(hntc, baseUrl, apiKey, secretKey, 4, 0, 4000, 29000, 1000000, 3190000);
        Thread tacWorker = new Main.XiePtcnThread(tac, baseUrl, apiKey, secretKey, 4, 0, 5000, 39000, 2000000, 5190000);
        worker.start();
        hntcWorker.start();
        tacWorker.start();
        try {
            worker.join();
            hntcWorker.join();
            tacWorker.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    static class XiePtcnThread extends Thread implements ResetController {

        final Instrument instrument;
        final BrokerApiRestClient client;
        final BhexInstrumentDepth depth;
        final IOrderBook book;
        final WebSocketDaemon wsDaemon;
        final int pricePrecision;
        final int sizePrecision;
        final int min;
        final int max;
        final int vMin;
        final int vMax;

        public XiePtcnThread(Instrument instrument, String baseUrl, String apiKey, String secret, int pricePrecision,
                int sizePrecision, int min, int max, int vMin, int vMax) {

            // AggregateOrderBook xieBook = app.makesureOrderBook(xie.asLong());
            // BhexInstrumentDepth
            // BaseWebSocketHandler hbexHandler = new BhexWebSocketHandler(new String[] {"XIEPTCN"}, new
            // BhexInstrumentDepth[] {xieDepth});
            // app.startWebSocket(Source.Bhex, hbexHandler);
            this.instrument = instrument;
            this.pricePrecision = pricePrecision;
            this.sizePrecision = sizePrecision;
            this.min = min;
            this.max = max;
            this.vMin = vMin;
            this.vMax = vMax;

            BrokerApiClientFactory factory = BrokerApiClientFactory.newInstance(baseUrl, apiKey, secret);
            client = factory.newRestClient();
            book = new OrderBook(instrument.asLong());
            depth = new BhexInstrumentDepth(instrument, book, Source.Bhex, this);
            BhexWebSocketHandler wsHandler = new BhexWebSocketHandler(new String[] { this.instrument.asString() },
                    new BhexInstrumentDepth[] { depth });
            wsDaemon = new WebSocketDaemon(wsHandler);
        }

        public void run() {
            wsDaemon.keepAlive();
            while (true) {
                long sleepMillis = InSpreadRunnable.getNumBetween(min, max);
                try {
                    Thread.sleep(sleepMillis);

                    InSpreadRunnable r = new InSpreadRunnable(instrument, book, client, pricePrecision, sizePrecision, vMin, vMax);
                    r.run();

                    wsDaemon.keepAlive();

                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }

        }

        @Override
        public void reset(Source mkt, Instrument instrument, BaseInstrumentDepth depth, boolean resubscribe,
                boolean reconnect) {

        }
    }

    public static String fmtDecimal(long v, int precision) {
        MathContext mc = new MathContext(String.valueOf(v).length(), RoundingMode.FLOOR);
        BigDecimal dec = BigDecimal.valueOf(v).divide(BigDecimal.valueOf(Math.pow(10, precision)), mc);
        // System.out.println(dec);
        return new DecimalFormat(StringUtils.rightPad("0.", 2 + precision, "0")).format(dec);
    }
}
