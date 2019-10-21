package com.kmfrog.martlet.tac;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kmfrog.martlet.C;
import com.kmfrog.martlet.book.IOrderBook;
import com.kmfrog.martlet.book.Instrument;

import io.broker.api.client.BrokerApiRestClient;
import io.broker.api.client.domain.account.NewOrder;
import io.broker.api.client.domain.account.NewOrderResponse;
import io.broker.api.client.domain.account.Order;
import io.broker.api.client.domain.account.TimeInForce;
import io.broker.api.client.domain.account.Trade;
import io.broker.api.client.domain.account.request.MyTradeRequest;
import io.broker.api.client.domain.account.request.OpenOrderRequest;

public class InSpreadRunnable implements Runnable {

    public static final int MIN_24H_VOLUME_PTCN = 260000;
    public static final int MAX_24H_VOLUME_PTCN = 320000;
    public static final int MIN_MIN_VOLUME_PTCN = 260000 / (24 * 60);  //394537
    public static final int MAX_MIN_VOLUME_PTCN = 320000 / (24 * 60);  //485583

    private Instrument instrument;
    private IOrderBook book;
    private BrokerApiRestClient client;

    private int ordPricePrecision;
    private int ordSizePrecision;
    
    private static Logger logger = LoggerFactory.getLogger(InSpreadRunnable.class);

    public InSpreadRunnable(Instrument instrument, IOrderBook book, BrokerApiRestClient client, int ordPricePrecision,
            int ordSizePrecision) {
        this.instrument = instrument;
        this.book = book;
        this.client = client;
        this.ordPricePrecision = ordPricePrecision;
        this.ordSizePrecision = ordSizePrecision;
    }

    @Override
    public void run() {
        long bid1 = book.getBestBidPrice();
        long ask1 = book.getBestAskPrice();
        long price1 = ask1 - getNumBetween(1, ask1 - bid1);
        long price2 = bid1 + getNumBetween(2, ask1 - bid1);
        long price = Math.max(price1, price2);
        
//        if(ask1 - bid1 == )

        if ( price < ask1) {
            // BigDecimal[] volumes = getVolume();
            // int compareValue = volumes[0].compareTo(volumes[1]);
            // BigDecimal sum = compareValue > 0 ? volumes[0] : volumes[1];
            // compareValue = volumes[2].compareTo(volumes[3]);
            // BigDecimal lastMinSum = compareValue > 0 ? volumes[2] : volumes[3];
            //
            // BigDecimal lastMinUplimit = getLastMinMax();
            // BigDecimal uplimitIn24h = get24hMax();
            //
            // if (sum.compareTo(uplimitIn24h)< 0 && lastMinSum.compareTo(lastMinUplimit) < 0) {
            // 最后一分种交易量小于预设值上限
            long quantity = getNumBetween(410000, 1570000);

            String qtyStr = fmtDec(quantity, instrument.getSizeFractionDigits(), ordSizePrecision);
            String priceStr = fmtDec(price, instrument.getPriceFractionDigits(), ordPricePrecision);
            NewOrder sell = NewOrder.limitSell(instrument.asString(), TimeInForce.GTC, qtyStr, priceStr);
            NewOrder buy = NewOrder.limitBuy(instrument.asString(), TimeInForce.GTC, qtyStr, priceStr);
            // System.out.println(sell+"\n####\n"+buy);
            NewOrderResponse sellResp, buyResp;
            if (System.currentTimeMillis() % 3 < 2) {
                sellResp = client.newOrder(sell);
                buyResp = client.newOrder(buy);
            }
            else {
                buyResp = client.newOrder(buy);
                sellResp = client.newOrder(sell);
            }
            
            // System.out.println(sellResp + "\n######\n"+buyResp);
            Set<Long> orderIds = new HashSet<>();
            orderIds.add(sellResp.getOrderId());
            orderIds.add(buyResp.getOrderId());
            

            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            OpenOrderRequest symbolOpenOrders = new OpenOrderRequest();
            symbolOpenOrders.setSymbol(instrument.asString());
            List<Order> openOrders = client.getOpenOrders(symbolOpenOrders);
            if (openOrders.size() > 0 && openOrders.stream().anyMatch(ord -> orderIds.contains(ord.getOrderId()))) {
               logger.warn("not matches {}|{}|{}, {}|{}|{}", sellResp.getOrderId(), sell.getPrice(), sell.getQuantity(),
                       buyResp.getOrderId(), buy.getPrice(), buy.getQuantity());
            }

            // }
        }

    }

    public BigDecimal[] getVolume() {
        MyTradeRequest req = new MyTradeRequest();
        long currentTimeMillis = System.currentTimeMillis();
        req.setStartTime(currentTimeMillis - 86400000L);
        List<Trade> tradeList = client.getMyTrades(req);
        BigDecimal buySum = new BigDecimal(0);
        BigDecimal sellSum = new BigDecimal(0);
        BigDecimal lastMinBuySum = new BigDecimal(0);
        BigDecimal lastMinSellSum = new BigDecimal(0);
        for (Trade t : tradeList) {
            BigDecimal qty = new BigDecimal(t.getQty());
            if (t.isBuyer()) {
                buySum.add(qty);
                if (t.getTime() + 60000 >= currentTimeMillis) {
                    lastMinBuySum.add(qty);
                }
            } else {
                sellSum.add(qty);
                if (t.getTime() + 60000 >= currentTimeMillis) {
                    lastMinSellSum.add(qty);
                }
            }
        }

        return new BigDecimal[] { buySum, sellSum, lastMinBuySum, lastMinSellSum };
    }

    /**
     * 获得随机数。
     * 
     * @param start 起始值，含。
     * @param end 结束值，不含。
     * @return
     */
    public static long getNumBetween(long start, long end) {
        Random rnd = new Random(System.currentTimeMillis());
        return (long) (start + rnd.nextDouble() * (end - start));
    }

    public static BigDecimal getLastMinMax() {
        long lastMinMax = getNumBetween(MIN_MIN_VOLUME_PTCN, MAX_MIN_VOLUME_PTCN);
        return BigDecimal.valueOf(lastMinMax);
    }

    public static BigDecimal get24hMax() {
        long max = getNumBetween(MIN_24H_VOLUME_PTCN, MAX_24H_VOLUME_PTCN);
        return BigDecimal.valueOf(max);
    }

    public static BigDecimal toDec(long v, int fractionDigits, int precision) {
        MathContext mc = new MathContext((int) (String.valueOf(v).length() - fractionDigits + precision),
                RoundingMode.FLOOR);
        return BigDecimal.valueOf(v).divide(BigDecimal.valueOf(C.POWERS_OF_TEN[fractionDigits]), mc);
    }

    public static String fmtDec(long v, int digits, int precision) {
        if (precision == 0) {
            return String.valueOf(v / C.POWERS_OF_TEN[digits]);
        }
        MathContext mc = new MathContext((int) (String.valueOf(v).length() - digits + precision), RoundingMode.FLOOR);
        BigDecimal dec = BigDecimal.valueOf(v).divide(BigDecimal.valueOf(C.POWERS_OF_TEN[digits]), mc);
        // System.out.println(dec);
        return new DecimalFormat(StringUtils.rightPad("0.", 2 + precision, "0")).format(dec);
    }

}
