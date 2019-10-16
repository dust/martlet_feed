package com.kmfrog.martlet.feed.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.Side;
import com.kmfrog.martlet.feed.BaseInstrumentDepth;
import com.kmfrog.martlet.feed.ResetController;
import com.kmfrog.martlet.feed.Source;

public class BhexInstrumentDepth extends BaseInstrumentDepth {
    
    private final AtomicLong lastUpdateId;
    private final AtomicLong lastUpdateTime;
    private final Lock lock;

    public BhexInstrumentDepth(Instrument instrument, AggregateOrderBook book, Source source,
            ResetController controller) {
        super(instrument, book, source, controller);
        
        lastUpdateId = new AtomicLong(0L);
        lastUpdateTime = new AtomicLong(0L);
        lock = new ReentrantLock();
        
    }

    @Override
    public void onMessage(String msg) {
        // have not used.
    }

    @Override
    public void onJSON(JSONObject root) {
        if(root.containsKey("data")) {
            JSONArray data = root.getJSONArray("data");
            JSONObject main = data.getJSONObject(0);
            boolean isSnapshot = root.getBooleanValue("f");
            
            long t = main.getLongValue("t");
            long id = Long.valueOf(main.getString("v").replace("_", ""));
            JSONArray bids = main.getJSONArray("b");
            JSONArray asks = main.getJSONArray("a");
            
            lock.lock();
            try {
                if (id > lastUpdateId.get()) {
                    
                    if(isSnapshot) {
                        book.clearSource(Side.BUY, source.ordinal());
                        book.clearSource(Side.SELL, source.ordinal());
                    }
                    
                    updatePriceLevel(Side.BUY, bids);
                    updatePriceLevel(Side.SELL, asks);
                    // logger.info("onMessage. {}|{}|{}, {}", lastUpdateId.get(), evtFirstId, evtLastId, lastId);
                    lastUpdateId.set(id);
                    lastUpdateTime.set(t);
                }
            }
            finally {
                lock.unlock();
            }
            
        }
    }

    @Override
    public void onReset(int errCode, String reason) {

    }

    @Override
    public Object onSnapshot(String snap) {
        // TODO Auto-generated method stub
        return null;
    }

}
