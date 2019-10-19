package com.kmfrog.martlet.book;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.unimi.dsi.fastutil.longs.Long2LongRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

/**
 * An basic order book. 一个线程安全的基本订单簿。
 */
public class OrderBook implements IOrderBook {
    private final long instrument;

    private final Long2LongRBTreeMap bids;
    private final Long2LongRBTreeMap asks;

    private final ReadWriteLock bidLock;
    private final ReadWriteLock askLock;

    public OrderBook(long instrument) {
        this.instrument = instrument;

        bids = new Long2LongRBTreeMap(LongComparators.OPPOSITE_COMPARATOR);
        asks = new Long2LongRBTreeMap(LongComparators.NATURAL_COMPARATOR);

        bidLock = new ReentrantReadWriteLock();
        askLock = new ReentrantReadWriteLock();
    }

    /**
     * Get the instrument.
     *
     * @return the instrument
     */
    public long getInstrument() {
        return instrument;
    }

    /**
     * Get the best bid price.
     *
     * @return the best bid price or zero if there are no bids
     */
    public long getBestBidPrice() {
        bidLock.readLock().lock();
        try {
            if (bids.isEmpty()) {
                return 0;
            }

            return bids.firstLongKey();
        } finally {
            bidLock.readLock().unlock();
        }
    }

    /**
     * Get the bid prices.
     *
     * @return the bid prices
     */
    public LongSortedSet getBidPrices() {
        bidLock.readLock().lock();
        try {
            return bids.keySet();
        } finally {
            bidLock.readLock().unlock();
        }
    }

    /**
     * Get a bid level size.
     *
     * @param price the bid price
     * @return the bid level size
     */
    public long getBidSize(long price) {
        bidLock.readLock().lock();
        try {
            return bids.get(price);
        } finally {
            bidLock.readLock().unlock();
        }
    }

    /**
     * Get the best ask price.
     *
     * @return the best ask price or zero if there are no asks
     */
    public long getBestAskPrice() {
        askLock.readLock().lock();
        try {
            if (asks.isEmpty()) {
                return 0;
            }
            return asks.firstLongKey();
        } finally {
            askLock.readLock().unlock();
        }
    }

    /**
     * Get the ask prices.
     *
     * @return the ask prices
     */
    public LongSortedSet getAskPrices() {
        askLock.readLock().lock();
        try {
            return asks.keySet();
        } finally {
            askLock.readLock().unlock();
        }
    }

    /**
     * Get an ask level size.
     *
     * @param price the ask price
     * @return the ask level size
     */
    public long getAskSize(long price) {
        askLock.readLock().lock();
        try {
            return asks.get(price);
        } finally {
            askLock.readLock().unlock();
        }
    }

    public boolean replace(Side side, long price, long quantity, int source) {
        // bids or asks
        Long2LongRBTreeMap levels = getLevels(side);
        Lock lock = side == Side.BUY ? bidLock.writeLock() : askLock.writeLock();

        lock.lock();
        try {
            if (quantity > 0) {
                levels.put(price, quantity);
            } else {
                levels.remove(price);
            }
            return price == levels.firstLongKey();
        } finally {
            lock.unlock();
        }
    }

    public boolean incr(Side side, long price, long quantity, int source) {
        // bids or asks
        Long2LongRBTreeMap levels = getLevels(side);
        Lock lock = side == Side.BUY ? bidLock.writeLock() : askLock.writeLock();

        lock.lock();
        try {
            long oldSize = levels.get(price);
            long newSize = oldSize + quantity;

            boolean onBestLevel = price == levels.firstLongKey();

            if (newSize > 0)
                levels.put(price, newSize);
            else
                levels.remove(price);

            return onBestLevel;
        } finally {
            lock.unlock();
        }
    }

    public boolean clear(Side side, int source) {
        Long2LongRBTreeMap levels = getLevels(side);
        Lock lock = side == Side.BUY ? bidLock.writeLock() : askLock.writeLock();
        
        lock.lock();
        try {
            levels.clear();
            return true;
        } finally {
            lock.unlock();
        }
    }

    private Long2LongRBTreeMap getLevels(Side side) {
        return side == Side.BUY ? bids : asks;
    }

}
