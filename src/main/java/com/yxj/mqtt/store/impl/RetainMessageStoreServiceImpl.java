package com.yxj.mqtt.store.impl;

import com.yxj.mqtt.bean.RetainMessage;
import com.yxj.mqtt.store.RetainMessageStoreService;
import com.yxj.mqtt.utils.StrUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RetainMessageStoreServiceImpl implements RetainMessageStoreService {
    private ConcurrentHashMap<String, RetainMessage> retainMessageCache;

    @Override
    public void put(String topic, RetainMessage retainMessage) {
        retainMessageCache.put(topic, retainMessage);
    }

    @Override
    public RetainMessage get(String topic) {
        return retainMessageCache.get(topic);
    }

    @Override
    public void remove(String topic) {
        retainMessageCache.remove(topic);
    }

    @Override
    public boolean containsKey(String topic) {
        return retainMessageCache.containsKey(topic);
    }

    @Override
    public List<RetainMessage> search(String topicFilter) {
        List<RetainMessage> retainMessageStores = new ArrayList<RetainMessage>();
        if (!StrUtil.contains(topicFilter, '#') && !StrUtil.contains(topicFilter, '+')) {
            if (retainMessageCache.containsKey(topicFilter)) {
                retainMessageStores.add(retainMessageCache.get(topicFilter));
            }
        } else {
            for (Map.Entry<String, RetainMessage> entry : retainMessageCache.entrySet()) {
                String topic = entry.getKey();
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
                        RetainMessage retainMessageStore = entry.getValue();
                        retainMessageStores.add(retainMessageStore);
                    }
                }
            };
        }
        return retainMessageStores;
    }
}
