package com.kmfrog.martlet.feed.impl;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.book.Side;
import com.kmfrog.martlet.feed.BaseInstrumentDepth;
import com.kmfrog.martlet.feed.ResetController;
import com.kmfrog.martlet.feed.Source;

/**
 * 币安深度
 * @author dust Oct 10, 2019
 *
 */
public class BinanceInstrumentDepth extends BaseInstrumentDepth {
    
    private final AtomicLong lastUpdateId;
    private final AtomicLong lastSnapshotId;
//    private final int MAX_DEPTH = 100;
    private ReentrantLock lock = new ReentrantLock();

    public BinanceInstrumentDepth(Instrument instrument, AggregateOrderBook book, Source source,
            ResetController controller) {
        super(instrument, book, source, controller);
        lastUpdateId = new AtomicLong(0);
        lastSnapshotId = new AtomicLong(0);
    }

    @Override
    public void onMessage(String msg) {
        //not use.
    }

    @Override
    public void onJSON(JSONObject root) {
        String evtName = root.getString("e");
        String symbol = root.getString("s");
        long evtFirstId = root.getLongValue("U");
        long evtLastId = root.getLongValue("u");
        // logger.info("onJSON {}|{}|{}, {}",lastUpdateId.get(), evtFirstId, evtLastId, lastSnapshotId.get());
        if (!evtName.equalsIgnoreCase("depthUpdate") || !symbol.equalsIgnoreCase(instrument.asString())) {
            return;
        }

        lock.lock();
        try {
            // long evtTime = root.getLongValue("E");
            JSONArray bids = root.getJSONArray("b");
            JSONArray asks = root.getJSONArray("a");

            long lastId = lastUpdateId.get();
            if (lastId == 0) {
                // 第一次收到event, lastId使用快照的lastUpdateId
                lastId = lastSnapshotId.get();
            }
            if (evtFirstId > lastId + 1) {
                // event第一个事件id比本地副本大，本地副本（快照)过旧.
                logger.info("a stale local copy. {}|{}|{}, {}", lastUpdateId.get(), evtFirstId, evtLastId, lastId);
                reset(true, false);
            }
            if (evtLastId < lastId + 1) {
                logger.info("a stale event. {}|{}|{}, {}", lastUpdateId.get(), evtFirstId, evtLastId, lastId);
                // 或者最后一个事件id比本地副本小。丢弃，等待event增长。
                return;
            }
            updatePriceLevel(Side.BUY, bids);
            updatePriceLevel(Side.SELL, asks);
            // logger.info("onMessage. {}|{}|{}, {}", lastUpdateId.get(), evtFirstId, evtLastId, lastId);
            lastUpdateId.set(evtLastId);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onReset(int errCode, String reason) {
        reset(true, true);
    }

    @Override
    public Object onSnapshot(String snap) {
        DefaultJSONParser parser = new DefaultJSONParser(snap);
        try {
            JSONObject root = parser.parseObject();

            JSONArray bids = root.getJSONArray("bids");
            JSONArray asks = root.getJSONArray("asks");
            // System.out.println("\n~~~~~~~~~~~~~~\n");
            // System.out.println(bids);
            // System.out.println("\n%%%%%%%%%%%%%%%%%%%%\n");
            lock.lock();
            try {
                if (lastSnapshotId.get() == 0L) {
                    updatePriceLevel(Side.BUY, bids);
                    updatePriceLevel(Side.SELL, asks);
                    lastSnapshotId.set(root.getLongValue("lastUpdateId"));
                }
            } finally {
                lock.unlock();
            }
            logger.info("snapshot: {}", lastSnapshotId.get());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        } finally {
            parser.close();
        }
        // book.dump(Side.BUY, System.out);
        // System.out.println("######\n");
        // book.dump(Side.SELL, System.out);
        // book.clearSource(Side.BUY, mkt.ordinal());
        // System.out.println("$$$$$$$$\n");
        // book.dump(Side.BUY, System.out);
        return null;
    }

    private void reset(boolean isRest, boolean isWs) {
        lock.lock();
        try {
            if (isRest) {
                lastSnapshotId.set(0);
                lastUpdateId.set(0);
                book.clearSource(Side.BUY, source.ordinal());
                book.clearSource(Side.SELL, source.ordinal());
            }

            if (isWs && !isRest) {
                lastUpdateId.set(0);
            }
        } finally {
            lock.unlock();
        }

        resetController.reset(source, instrument, this, isRest, isWs);
        logger.info("reset");

    }

    

}
