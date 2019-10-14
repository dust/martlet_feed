package com.kmfrog.martlet.feed;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.Instrument;

/**
 * 币对交易深度
 * @author dust Oct 14, 2019
 *
 */
public abstract class BaseInstrumentDepth implements WsDataListener, SnapshotDataListener {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected final AggregateOrderBook book;
    protected final Instrument instrument;
    protected final Source source;
    protected final ResetController resetController;
    
    /**
     * 深度最后更新时间。
     */
    protected final AtomicLong lastTimestamp;

    public BaseInstrumentDepth(Instrument instrument, AggregateOrderBook book, Source source,
            ResetController controller) {
        this.instrument = instrument;
        this.book = book;
        this.source = source;
        this.resetController = controller;
        
        lastTimestamp = new AtomicLong(0L);

    }

}
