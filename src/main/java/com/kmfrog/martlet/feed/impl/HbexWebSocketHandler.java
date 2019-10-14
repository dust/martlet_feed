package com.kmfrog.martlet.feed.impl;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.math3.util.Pair;
import org.eclipse.jetty.websocket.api.Session;

import com.kmfrog.martlet.feed.BaseWebSocketHandler;

public class HbexWebSocketHandler extends BaseWebSocketHandler {
    
    String wsUrl;
    List<Pair<String,String>> symbolList;
    

    public HbexWebSocketHandler(String wsUrlFmt, List<Pair<String, String>> pairs) {
        super();
        this.symbolNames = ConcurrentHashMap.newKeySet();
        this.symbolList = pairs;
//        this.symbolNames = Collectors.toList(pairs.stream().colp->String.format("%s%s", p.getKey(), p.getValue())));
        
    }

    @Override
    public String getWebSocketUrl() {
        return wsUrl;
    }
    
    

    @Override
    public void onConnect(Session session) {
        try {
        session.getRemote().sendString("{\n" + 
                "  \"symbol\": \"\",\n" + 
                "  \"topic\": \"depth\",\n" + 
                "  \"event\": \"sub\",\n" + 
                "  \"params\": {\n" + 
                "    \"binary\": false\n" + 
                "    }\n" + 
                "}" );
        }
        catch(Exception ex) {
            logger.warn(ex.getMessage(), ex);
        }
    }

    @Override
    protected void onMessage(String msg) {
        logger.debug("{},{}", wsUrl, msg);
    }

}
