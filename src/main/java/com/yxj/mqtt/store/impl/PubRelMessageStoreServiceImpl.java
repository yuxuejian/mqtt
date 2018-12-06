package com.yxj.mqtt.store.impl;

import com.yxj.mqtt.bean.PubRelMessage;
import com.yxj.mqtt.store.MessageIdService;
import com.yxj.mqtt.store.PubRelMessageStoreService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PubRelMessageStoreServiceImpl implements PubRelMessageStoreService {
    @Autowired
    private MessageIdService messageIdService;

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, PubRelMessage>> pubRelMessageCache;

    @Override
    public void put(String clientId, PubRelMessage pubRelMessage) {
        ConcurrentHashMap<Integer, PubRelMessage> map = pubRelMessageCache.containsKey(clientId) ? pubRelMessageCache.get(clientId) : new ConcurrentHashMap<Integer, PubRelMessage>();
        map.put(pubRelMessage.getMessageId(), pubRelMessage);
        pubRelMessageCache.put(clientId, map);
    }

    @Override
    public List<PubRelMessage> get(String clientId) {
        if (pubRelMessageCache.containsKey(clientId)) {
            ConcurrentHashMap<Integer, PubRelMessage> map = pubRelMessageCache.get(clientId);
            Collection<PubRelMessage> collection = map.values();
            return new ArrayList<PubRelMessage>(collection);
        }
        return new ArrayList<PubRelMessage>();
    }

    @Override
    public void remove(String clientId, int messageId) {
        if (pubRelMessageCache.containsKey(clientId)) {
            ConcurrentHashMap<Integer, PubRelMessage> map = pubRelMessageCache.get(clientId);
            if (map.containsKey(messageId)) {
                map.remove(messageId);
                if (map.size() > 0) {
                    pubRelMessageCache.put(clientId, map);
                } else {
                    pubRelMessageCache.remove(clientId);
                }
            }
        }
    }

    @Override
    public void removeByClient(String clientId) {
        if (pubRelMessageCache.containsKey(clientId)) {
            ConcurrentHashMap<Integer, PubRelMessage> map = pubRelMessageCache.get(clientId);
            map.forEach((messageId, dupPubRelMessageStore) -> {
                messageIdService.releaseMessageId(messageId);
            });
            map.clear();
            pubRelMessageCache.remove(clientId);
        }
    }
}
