package com.kmfrog.martlet.book;

import it.unimi.dsi.fastutil.longs.LongSortedSet;

/**
 * 订单簿接口。
 * @author dust Sep 30, 2019
 *
 */
public interface IOrderBook {

    long getInstrument();
    long getBestBidPrice();
    LongSortedSet getBidPrices();
    long getBidSize(long price);
    
    long getBestAskPrice();
    LongSortedSet getAskPrices();
    long getAskSize(long price);
    
    boolean add(Side side, long price, long quantity, int source);
    boolean update(Side side, long price, long quantity, int source);
}
