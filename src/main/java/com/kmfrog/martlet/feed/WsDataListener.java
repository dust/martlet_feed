package com.kmfrog.martlet.feed;

/**
 * websocket数据监听器。
 * @author dust Oct 11, 2019
 *
 */
public interface WsDataListener {

    Object onMessage(String msg);
    
    void onReset(int errCode, String reason);

}
