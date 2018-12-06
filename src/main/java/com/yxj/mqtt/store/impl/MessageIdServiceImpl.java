package com.yxj.mqtt.store.impl;

import com.yxj.mqtt.store.MessageIdService;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageIdServiceImpl implements MessageIdService {
    private final int MIN_MSG_ID = 1;

    private final int MAX_MSG_ID = 65535;

    private Lock lock1 = new ReentrantLock();

    private final int lock = 0;

    @Resource
    private ConcurrentHashMap<Integer, Integer> messageIdCache;

    private int nextMsgId = MIN_MSG_ID - 1;

    @Override
    public int getNextMessageId() {
        lock1.lock();
        try {
            do {
                nextMsgId++;
                if (nextMsgId > MAX_MSG_ID) {
                    nextMsgId = MIN_MSG_ID;
                }
            } while (messageIdCache.containsKey(nextMsgId));
            messageIdCache.put(nextMsgId, nextMsgId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock1.unlock();
        }
        return nextMsgId;
    }

    @Override
    public void releaseMessageId(int messageId) {
        lock1.lock();
        try {
            messageIdCache.remove(messageId);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock1.unlock();
        }
    }
}
