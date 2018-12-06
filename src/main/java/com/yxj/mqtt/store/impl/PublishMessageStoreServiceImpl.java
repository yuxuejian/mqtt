package com.yxj.mqtt.store.impl;

import com.yxj.mqtt.bean.PublishMessage;
import com.yxj.mqtt.store.MessageIdService;
import com.yxj.mqtt.store.PublishMessageStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PublishMessageStoreServiceImpl implements PublishMessageStoreService {

    @Autowired
    private MessageIdService messageIdService;

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, PublishMessage>> publishMessageCache = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, PublishMessage publishMessage) {
        ConcurrentHashMap<Integer, PublishMessage> map = publishMessageCache.containsKey(clientId) ? publishMessageCache.get(clientId) : new ConcurrentHashMap<>();
        map.put(publishMessage.getMessageId(), publishMessage);
        publishMessageCache.put(clientId, map);
    }

    @Override
    public List<PublishMessage> get(String clientId) {
        if (publishMessageCache.containsKey(clientId)) {
            ConcurrentHashMap<Integer, PublishMessage> map = publishMessageCache.get(clientId);
            Collection<PublishMessage> collection = map.values();
            return new ArrayList<>(collection);
        }
        return new ArrayList<PublishMessage>();
    }

    @Override
    public void remove(String clientId, int messageId) {
        if (publishMessageCache.containsKey(clientId)) {
            ConcurrentHashMap<Integer, PublishMessage> map = publishMessageCache.get(clientId);
            if (map.containsKey(messageId)) {
                map.remove(messageId);
                if (map.size() > 0) {
                    publishMessageCache.put(clientId, map);
                } else {
                    publishMessageCache.remove(clientId);
                }
            }
        }
    }

    @Override
    public void removeByClient(String clientId) {
        if (publishMessageCache.containsKey(clientId)) {
            ConcurrentHashMap<Integer, PublishMessage> map = publishMessageCache.get(clientId);
            map.forEach((messageId, dupPublishMessageStore) -> {
                messageIdService.releaseMessageId(messageId);
            });
            map.clear();
            publishMessageCache.remove(clientId);
        }
    }
}
