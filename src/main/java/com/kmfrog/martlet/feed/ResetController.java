package com.kmfrog.martlet.feed;

import com.kmfrog.martlet.book.Instrument;
import com.kmfrog.martlet.feed.impl.BinanceInstrumentDepth;

/**
 * 重置控制器接口。用于将内部状态传导至外部控制者。
 * @author dust Oct 11, 2019
 *
 */
public interface ResetController {
    
    void reset(Source mkt, Instrument instrument, BinanceInstrumentDepth depth, boolean isSnapshot, boolean isWs);

}
