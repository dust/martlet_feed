package com.kmfrog.martlet.book;

import java.io.PrintStream;

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
    
    long getLastUpdateTs();
    long getLastReceivedTs();
    void setLastUpdateTs(long ts);
    
    /**
     * 将参数中的变化量累计到订单档位，不存在此档位时会新增。
     * @param side
     * @param price
     * @param delta 变化量，`oldSize` + `delta`小于零时，将会删除此档位。
     * @param source 来源，非聚合订单簿时，此参数并不使用。
     * @return 如果最优档发生变化，返回`true`，否则`false`
     */
    boolean incr(Side side, long price, long delta, int source);
    
    /**
     * 将参数的值直接更新（替换）掉旧档档，不存在此档位时会新增。
     * @param side
     * @param price
     * @param quantity
     * @param source 来源，非聚合订单簿时，此参数并不使用。
     * @return 如果最优档发生变化，返回`true`，否则`false`
     */
    boolean replace(Side side, long price, long quantity, int source);
    
    /**
     * 从订单簿中清理某个来源的订单项。
     * @param side
     * @param source
     * @return
     */
    boolean clear(Side side, int source);
    
    /**
     * 输出某一侧的订单列表。
     * @param side
     * @param ps
     */
    void dump(Side side, PrintStream ps);
}
