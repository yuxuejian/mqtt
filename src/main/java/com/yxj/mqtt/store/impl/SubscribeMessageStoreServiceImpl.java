package com.yxj.mqtt.store.impl;

import com.yxj.mqtt.bean.SubscribeMessage;
import com.yxj.mqtt.store.SubscribeMessageStoreService;
import com.yxj.mqtt.utils.StrUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SubscribeMessageStoreServiceImpl implements SubscribeMessageStoreService {
    private ConcurrentHashMap<String, ConcurrentHashMap<String, SubscribeMessage>> subscribeNotWildcardCache;

    private ConcurrentHashMap<String, ConcurrentHashMap<String, SubscribeMessage>> subscribeWildcardCache;

    @Override
    public void put(String topicFilter, SubscribeMessage subscribeMessage) {
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            ConcurrentHashMap<String, SubscribeMessage> map =
                    subscribeWildcardCache.containsKey(topicFilter) ? subscribeWildcardCache.get(topicFilter) : new ConcurrentHashMap<String, SubscribeMessage>();
            map.put(subscribeMessage.getClientId(), subscribeMessage);
            subscribeWildcardCache.put(topicFilter, map);
        } else {
            ConcurrentHashMap<String, SubscribeMessage> map =
                    subscribeNotWildcardCache.containsKey(topicFilter) ? subscribeNotWildcardCache.get(topicFilter) : new ConcurrentHashMap<String, SubscribeMessage>();
            map.put(subscribeMessage.getClientId(), subscribeMessage);
            subscribeNotWildcardCache.put(topicFilter, map);
        }
    }

    @Override
    public void remove(String topicFilter, String clientId) {
        if (StrUtil.contains(topicFilter, '#') || StrUtil.contains(topicFilter, '+')) {
            if (subscribeWildcardCache.containsKey(topicFilter)) {
                ConcurrentHashMap<String, SubscribeMessage> map = subscribeWildcardCache.get(topicFilter);
                if (map.containsKey(clientId)) {
                    map.remove(clientId);
                    if (map.size() > 0) {
                        subscribeWildcardCache.put(topicFilter, map);
                    } else {
                        subscribeWildcardCache.remove(topicFilter);
                    }
                }
            }
        } else {
            if (subscribeNotWildcardCache.containsKey(topicFilter)) {
                ConcurrentHashMap<String, SubscribeMessage> map = subscribeNotWildcardCache.get(topicFilter);
                if (map.containsKey(clientId)) {
                    map.remove(clientId);
                    if (map.size() > 0) {
                        subscribeNotWildcardCache.put(topicFilter, map);
                    } else {
                        subscribeNotWildcardCache.remove(topicFilter);
                    }
                }
            }
        }
    }

    @Override
    public void removeForClient(String clientId) {
        for (Map.Entry<String, ConcurrentHashMap<String, SubscribeMessage>> entry : subscribeNotWildcardCache.entrySet()){
            ConcurrentHashMap<String, SubscribeMessage> map = entry.getValue();
            if (map.containsKey(clientId)) {
                map.remove(clientId);
                if (map.size() > 0) {
                    subscribeNotWildcardCache.put(entry.getKey(), map);
                } else {
                    subscribeNotWildcardCache.remove(entry.getKey());
                }
            }
        };
        for (Map.Entry<String, ConcurrentHashMap<String, SubscribeMessage>> entry : subscribeWildcardCache.entrySet()){
            ConcurrentHashMap<String, SubscribeMessage> map = entry.getValue();
            if (map.containsKey(clientId)) {
                map.remove(clientId);
                if (map.size() > 0) {
                    subscribeWildcardCache.put(entry.getKey(), map);
                } else {
                    subscribeWildcardCache.remove(entry.getKey());
                }
            }
        };
    }

    @Override
    public List<SubscribeMessage> search(String topic) {
        List<SubscribeMessage> subscribeStores = new ArrayList<SubscribeMessage>();
        if (subscribeNotWildcardCache.containsKey(topic)) {
            ConcurrentHashMap<String, SubscribeMessage> map = subscribeNotWildcardCache.get(topic);
            Collection<SubscribeMessage> collection = map.values();
            List<SubscribeMessage> list = new ArrayList<SubscribeMessage>(collection);
            subscribeStores.addAll(list);
        }
        for (Map.Entry<String, ConcurrentHashMap<String, SubscribeMessage>> entry : subscribeWildcardCache.entrySet()){
            String topicFilter = entry.getKey();
            if (StrUtil.split(topic, '/').size() >= StrUtil.split(topicFilter, '/').size()) {
                List<String> splitTopics = StrUtil.split(topic, '/');
                List<String> spliteTopicFilters = StrUtil.split(topicFilter, '/');
                String newTopicFilter = "";
                for (int i = 0; i < spliteTopicFilters.size(); i++) {
                    String value = spliteTopicFilters.get(i);
                    if (value.equals("+")) {
                        newTopicFilter = newTopicFilter + "+/";
                    } else if (value.equals("#")) {
                        newTopicFilter = newTopicFilter + "#/";
                        break;
                    } else {
                        newTopicFilter = newTopicFilter + splitTopics.get(i) + "/";
                    }
                }
                newTopicFilter = StrUtil.removeSuffix(newTopicFilter, "/");
                if (topicFilter.equals(newTopicFilter)) {
                    ConcurrentHashMap<String, SubscribeMessage> map = entry.getValue();
                    Collection<SubscribeMessage> collection = map.values();
                    List<SubscribeMessage> list = new ArrayList<SubscribeMessage>(collection);
                    subscribeStores.addAll(list);
                }
            }
        };
        return subscribeStores;
    }

}
