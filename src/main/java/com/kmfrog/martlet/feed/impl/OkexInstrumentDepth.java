package com.kmfrog.martlet.feed.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.Side;
import com.kmfrog.martlet.feed.BaseInstrumentDepth;
import com.kmfrog.martlet.feed.ResetController;
import com.kmfrog.martlet.feed.Source;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author dust
 */
public class OkexInstrumentDepth extends BaseInstrumentDepth {

    private final AtomicLong lastTimestamp;
    // private final int MAX_DEPTH = 100;
    private ReentrantLock lock = new ReentrantLock();

    public OkexInstrumentDepth(Instrument instrument, AggregateOrderBook book, Source source, ResetController controller) {
        super(instrument, book, source, controller);
        lastTimestamp = new AtomicLong(0L);
    }

    @Override
    public void onJSON(JSONObject root, boolean isSnapshot) {

        long ts = root.getDate("timestamp").getTime();

        lock.lock();
        try{
            if(ts < lastTimestamp.get() && !isSnapshot){
                return;
            }
            JSONArray bids = root.getJSONArray("bids");
            JSONArray asks = root.getJSONArray("asks");

            if(isSnapshot){
                book.clearSource(Side.BUY, source.ordinal());
                book.clearSource(Side.SELL, source.ordinal());
            }

            updatePriceLevel(Side.BUY, bids);
            updatePriceLevel(Side.SELL, asks);
            lastTimestamp.set(ts);

        }
        finally {
            lock.unlock();
        }

    }
}
