package com.kmfrog.martlet.feed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.Instrument;

public abstract class BaseInstrumentDepth implements WsDataListener, SnapshotDataListener {

    protected Logger logger = LoggerFactory.getLogger(getClass());
    protected final AggregateOrderBook book;
    protected final Instrument instrument;
    protected final Source source;
    protected final ResetController resetController;

    public BaseInstrumentDepth(Instrument instrument, AggregateOrderBook book, Source source,
            ResetController controller) {
        this.instrument = instrument;
        this.book = book;
        this.source = source;
        this.resetController = controller;

    }

}
