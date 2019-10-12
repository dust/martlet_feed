package com.kmfrog.martlet.book;

import java.io.PrintStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.LongComparators;
import it.unimi.dsi.fastutil.longs.LongSortedSet;

/**
 * An aggregate order book(Thread safety. 多源订单簿。它是线程安全的。
 * 
 * @author dust Sep 30, 2019
 *
 */
public class AggregateOrderBook /* implements IOrderBook */ {

    private final long instrument;
    private final Long2ObjectRBTreeMap<MultiSrc> bids;
    private final Long2ObjectRBTreeMap<MultiSrc> asks;
    private final ReadWriteLock bidLock;
    private final ReadWriteLock askLock;

    public AggregateOrderBook(long instrument) {
        this.instrument = instrument;

        this.bids = new Long2ObjectRBTreeMap<MultiSrc>(LongComparators.OPPOSITE_COMPARATOR);
        this.asks = new Long2ObjectRBTreeMap<MultiSrc>(LongComparators.NATURAL_COMPARATOR);

        this.bidLock = new ReentrantReadWriteLock();
        this.askLock = new ReentrantReadWriteLock();
    }

    /**
     * Get the instrument
     * 
     * @return
     */
    public long getInstrument() {
        return instrument;
    }

    /**
     * Is the bid order book empty?
     * 
     * @return
     */
    public boolean isBidEmpty() {
        bidLock.readLock().lock();
        try {
            return bids.isEmpty();
        } finally {
            bidLock.readLock().unlock();
        }
    }

    /**
     * Get the best bid price.
     * 
     * @return
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
     * @return
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
     * @param price
     * @return
     */
    public long getBidSize(long price) {
        bidLock.readLock().lock();
        try {
            return bids.get(price).size();
        } finally {
            bidLock.readLock().unlock();
        }
    }

    /**
     * Is the ask order book empty?
     * 
     * @return
     */
    public boolean isAskEmpty() {
        askLock.readLock().lock();
        try {
            return asks.isEmpty();
        } finally {
            askLock.readLock().unlock();
        }
    }

    /**
     * Get the best ask price.
     * 
     * @return
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
     * Get the ask prices;
     * 
     * @return
     */
    public long getBestAskPrices() {
        bidLock.readLock().lock();
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
     * Get an ask level size.
     */
    public long getAskSize(long price) {
        askLock.readLock().lock();
        try {
            return asks.get(price).size();
        } finally {
            askLock.readLock().unlock();
        }
    }

    /**
     * Get the ask prices.
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
     * add order from someone source.
     * 
     * @param side
     * @param price
     * @param quantity
     * @param source
     * @return the best bid or offer has change?
     */
    public boolean add(Side side, long price, long quantity, int source) {
        // bids or asks

        Lock lock = side == Side.BUY ? bidLock.writeLock() : askLock.writeLock();

        lock.lock();
        try {
            Long2ObjectRBTreeMap<MultiSrc> levels = getLevels(side);

            if (!levels.containsKey(price)) {
                MultiSrc multiSrc = new MultiSrc(price);
                multiSrc.addTo(quantity, source);
                levels.put(price, multiSrc);
            } else {
                levels.get(price).addTo(quantity, source);
            }

            return price == levels.firstLongKey();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 增量更新某个来源的订单数。
     * 
     * @param side
     * @param price
     * @param quantity
     * @param source
     * @return
     */
    public boolean update(Side side, long price, long quantity, int source) {
        Lock lock = side == Side.BUY ? bidLock.writeLock() : askLock.writeLock();

        lock.lock();
        try {
            Long2ObjectRBTreeMap<MultiSrc> levels = getLevels(side);

            if (!levels.containsKey(price)) {
                // 如果order book中不存在数量为零的此价位，那么也没有意义进行添加。
                if (quantity > 0L) {
                    MultiSrc multiSrc = new MultiSrc(price);
                    multiSrc.updateTo(quantity, source);
                    levels.put(price, multiSrc);
                }
            } else {
                long newSize = levels.get(price).updateTo(quantity, source);
                if (newSize <= 0) {
                    levels.remove(price);
                }
            }

            return levels.size() > 0 && price == levels.firstLongKey();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清理某个来源所有订单（价格，数量）。
     * 
     * @param side
     * @param source
     * @return
     */
    public boolean clearSource(Side side, int source) {
        Lock lock = side == Side.BUY ? bidLock.writeLock() : askLock.writeLock();

        lock.lock();
        try {
            Long2ObjectRBTreeMap<MultiSrc> levels = getLevels(side);
            if (levels.size() == 0) {
                return false;
            }

            long oldPrice = levels.firstLongKey();
            levels.forEach((k, v) -> {
                if (v.clearSource(source)) {
                    levels.remove(k.longValue());
                }
            });

            return levels.size() == 0 || oldPrice != levels.firstLongKey();
        } finally {
            lock.unlock();
        }
    }

    public void dump(Side side, PrintStream writer) {
        Lock lock = side == Side.BUY ? bidLock.readLock() : askLock.readLock();

        lock.lock();
        try {
            Long2ObjectRBTreeMap<MultiSrc> levels = getLevels(side);
            levels.forEach((k, v) -> {
                writer.printf("%d:[%s], ", k, v.dumpString());
            });
        } finally {
            lock.unlock();
        }
    }

    private Long2ObjectRBTreeMap<MultiSrc> getLevels(Side side) {
        return side == Side.BUY ? bids : asks;
    }

}
