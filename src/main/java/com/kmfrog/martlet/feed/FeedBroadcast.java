package com.kmfrog.martlet.feed;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import com.kmfrog.martlet.book.IOrderBook;

/**
 * ZeroMQ消息发布服务器。
 * @author dust Oct 25, 2019
 *
 */
public class FeedBroadcast {
    
    final ZMQ.Context ctx;
    final ZMQ.Socket publisher;
    
    
    public FeedBroadcast(String host, int port, int ioThreads) {
        ctx = ZMQ.context(ioThreads);
        publisher = ctx.socket(SocketType.PUB);
        publisher.bind(String.format("tcp://%s:%d", host, port));   
        
    }
    
    
    public void sendDepth(IOrderBook book, int pricePrecision, int volumePrecision, int maxLevel) {
        publisher.send(book.getPlainText(pricePrecision, volumePrecision, maxLevel));
    }
    
    public void destory() {
        publisher.close();
        ctx.close();
    }

}
