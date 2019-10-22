package com.kmfrog.martlet.feed;

import com.kmfrog.martlet.book.AggregateOrderBook;
import com.kmfrog.martlet.book.IOrderBook;

public class AggregateRunnable implements Runnable {

    private AggregateOrderBook book;
    private Source[] sources;
    private App app;

    AggregateRunnable(AggregateOrderBook book, Source[] sources, App app) {
        this.book = book;
        this.sources = sources;
        this.app = app;
    }
    
    @Override
    public void run() {
        for (Source src : sources) {
            // TODO: 检查不同来源的order book的边界偏离程度。 diff time, diff bbo
            IOrderBook srcBook = app.makesureOrderBook(src, book.getInstrument());
            
            book.aggregate(src, srcBook);
        }
    }

}
