package com.kmfrog.martlet.tac;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kmfrog.martlet.C;
import com.kmfrog.martlet.book.IOrderBook;
import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.OrderBook;
import com.kmfrog.martlet.feed.BaseInstrumentDepth;
import com.kmfrog.martlet.feed.ResetController;
import com.kmfrog.martlet.feed.Source;
import com.kmfrog.martlet.feed.WebSocketDaemon;
import com.kmfrog.martlet.feed.impl.BhexInstrumentDepth;
import com.kmfrog.martlet.feed.impl.BhexWebSocketHandler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import io.broker.api.client.BrokerApiClientFactory;
import io.broker.api.client.BrokerApiRestClient;
import io.broker.api.client.constant.BrokerConstants;
import io.broker.api.client.domain.account.Account;
import io.broker.api.client.domain.account.Order;
import io.broker.api.client.domain.account.OrderSide;
import io.broker.api.client.domain.account.Trade;
import io.broker.api.client.domain.account.request.MyTradeRequest;
import io.broker.api.client.domain.account.request.OpenOrderRequest;
import io.broker.api.client.domain.general.BrokerInfo;

public class Main {

    static Logger logger = LoggerFactory.getLogger(InSpreadRunnable.class);

    public Main() {

    }

    public static void main(String[] args) {
    	
    	 Config cfg = ConfigFactory.load();
         String baseUrl = cfg.getString("api.base.url");
         String apiKey = cfg.getString("api.key");
         String secretKey = cfg.getString("api.secret");

//         Main app = new Main();
//         
//         Instrument xie = new Instrument("XIEPTCN", 3, 3);
//        
//         BrokerApiClientFactory factory = BrokerApiClientFactory.newInstance(baseUrl, apiKey,
//        		 secretKey);
//         BrokerApiRestClient client = factory.newRestClient();
//         OrderBook xieBook = new OrderBook(xie.asLong());
//        
//         // NewOrderResponse resp = client.newOrder(NewOrder.limitBuy("XIEPTCN", TimeInForce.GTC, "10", "0.035"));
//         // System.out.println(resp);
//        
//         BrokerInfo info = client.getBrokerInfo();
//         try {
//         Thread.sleep(10000L);
//         } catch (InterruptedException e) {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//         }
//         InSpreadRunnable r = new InSpreadRunnable(xie, xieBook, client, 3, 0);
////         r.run();
//         BigDecimal[] vols = r.getVolume();
//         System.out.println(Arrays.asList(vols));
//        
//         List<Order> openOrders = client.getOpenOrders(new OpenOrderRequest("XIEPTCN", 100));
//         BigDecimal xieSum = new BigDecimal("0");
//         BigDecimal ptcnSum = new BigDecimal("0");
//         for(Order o : openOrders) {
//        	 if(o.getSide() == OrderSide.SELL) {
//        	 xieSum = xieSum.add(new BigDecimal(o.getOrigQty()));
//        	 ptcnSum = ptcnSum.add(new BigDecimal(o.getCummulativeQuoteQty()));
//        	 }
//         }
////         System.out.println(openOrders);
//         System.out.format("%s|%s\n", xieSum, ptcnSum);
//        
//         Account acct = client.getAccount(BrokerConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis());
//         System.out.println(acct);
//         System.out.println(acct.getBalances());
//         System.out.println(acct.getAssetBalance("PTCN"));
//        
//         List<Trade> trades = client.getMyTrades(new MyTradeRequest());
//         System.out.println(trades);
//        
//         // MathContext mc = new MathContext(4, RoundingMode.FLOOR);
//         // BigDecimal x = BigDecimal.valueOf(1000000000000L).divide(BigDecimal.valueOf(140000), mc );
//         // Instrument i = new Instrument("X", 8, 8);
//         // System.out.println(i.getPriceDecimal(10000000, 4));
//         // System.out.println(i.getPriceStr(10000000, 4));
//         System.out.println(new DecimalFormat("0.0").format(new BigDecimal("10000")));
//         System.out.println(fmtDecimal(86931, 3));

        // Random rnd = new Random(System.currentTimeMillis());
        // for(int i=0; i<100; i++) {
        // System.out.println((long)(21 + rnd.nextDouble() * (34 - 21)));
        // }

       
        Thread worker = new Main.XiePtcnThread(baseUrl, apiKey, secretKey);
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    static class XiePtcnThread extends Thread implements ResetController {

        static final Instrument XIE = new Instrument("XIEPTCN", 3, 3);
        final BrokerApiRestClient client;
        final BhexInstrumentDepth xieDepth;
        final IOrderBook book;
        final WebSocketDaemon wsDaemon;

        public XiePtcnThread(String baseUrl, String apiKey, String secret) {

            // AggregateOrderBook xieBook = app.makesureOrderBook(xie.asLong());
            // BhexInstrumentDepth
            // BaseWebSocketHandler hbexHandler = new BhexWebSocketHandler(new String[] {"XIEPTCN"}, new
            // BhexInstrumentDepth[] {xieDepth});
            // app.startWebSocket(Source.Bhex, hbexHandler);

            BrokerApiClientFactory factory = BrokerApiClientFactory.newInstance(baseUrl, apiKey, secret);
            client = factory.newRestClient();
            book = new OrderBook(XIE.asLong());
            xieDepth = new BhexInstrumentDepth(XIE, book, Source.Bhex, this);
            BhexWebSocketHandler wsHandler = new BhexWebSocketHandler(new String[] { "XIEPTCN" },
                    new BhexInstrumentDepth[] { xieDepth });
            wsDaemon = new WebSocketDaemon(wsHandler);
        }

        public void run() {
            wsDaemon.keepAlive();
            while (true) {
                long sleepMillis = InSpreadRunnable.getNumBetween(2000, 17000);
                try {
                    Thread.sleep(sleepMillis);

                    InSpreadRunnable r = new InSpreadRunnable(XIE, book, client, 3, 0);
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