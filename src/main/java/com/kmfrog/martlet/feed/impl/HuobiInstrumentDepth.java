package com.kmfrog.martlet.feed.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.Side;
import com.kmfrog.martlet.feed.BaseInstrumentDepth;
import com.kmfrog.martlet.feed.ResetController;
import com.kmfrog.martlet.feed.Source;

public class HuobiInstrumentDepth extends BaseInstrumentDepth {

    private final AtomicLong lastUpdateId;
    // private final int MAX_DEPTH = 100;
    private ReentrantLock lock = new ReentrantLock();

    public HuobiInstrumentDepth(Instrument instrument, AggregateOrderBook book, Source source,
            ResetController controller) {
        super(instrument, book, source, controller);
        lastUpdateId = new AtomicLong(0L);

    }

    @Override
    public void onMessage(String msg) {
        // not used.
    }

    @Override
    public void onJSON(JSONObject json, boolean isSnapshot) {
        lock.lock();
        try {
            long ts = json.getLongValue("ts");
            if (ts < lastUpdateId.get()) {
                return;
            }

            lastUpdateId.set(ts);
            JSONArray bids = json.getJSONArray("bids");
            JSONArray asks = json.getJSONArray("asks");

            book.clearSource(Side.BUY, source.ordinal());
            book.clearSource(Side.SELL, source.ordinal());

            updatePriceLevel(Side.BUY, bids);
            updatePriceLevel(Side.SELL, asks);
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void onReset(int errCode, String reason) {
        // TODO Auto-generated method stub

    }

    @Override
    public Object onSnapshot(String snap) {
        // TODO Auto-generated method stub
        return null;
    }

}
