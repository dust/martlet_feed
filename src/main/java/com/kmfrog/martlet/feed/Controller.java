package com.kmfrog.martlet.feed;

import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.OrderBook;

/**
 * 业务控制器接口。
 * 用于将内部状态传导至外部控制者。也用于内部调用顶层接口。
 * @author dust Oct 11, 2019
 *
 */
public interface Controller {
    
    /**
     * 重置某个来源
     * @param mkt
     * @param instrument
     * @param depth
     * @param resubscribe
     * @param reconnect
     */
    void reset(Source mkt, Instrument instrument, BaseInstrumentDepth depth, boolean resubscribe, boolean reconnect);
    
    /**
     * 将单一来源的order book交给控制层。由它来进行聚合或其它处理。
     * @param mkt
     * @param instrument
     * @param book
     */
    void aggregate(Source mkt, Instrument instrument, OrderBook book);
    
    /**
     * 因数据或其它原因，某个来源的数据已经无效了，提交给控制层，由它进行处理。
     * @param mkt
     * @param instrument
     * @param book
     */
    void clear(Source mkt, Instrument instrument, OrderBook book);

}
