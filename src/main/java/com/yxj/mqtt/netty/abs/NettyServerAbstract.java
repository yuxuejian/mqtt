package com.yxj.mqtt.netty.abs;

import com.yxj.mqtt.process.BrokerProcess;
import com.yxj.mqtt.thread.RequestTask;
import com.yxj.mqtt.utils.Pair;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.MqttMessage;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;

public abstract class NettyServerAbstract {
    protected final HashMap<Integer/* request code */, Pair<BrokerProcess, ExecutorService>> processorTable =
            new HashMap<Integer, Pair<BrokerProcess, ExecutorService>>(64);
    protected Pair<BrokerProcess, ExecutorService> defaultRequestProcessor;

    public void processMessageReceived(ChannelHandlerContext ctx, MqttMessage msg) {
        final Pair<BrokerProcess, ExecutorService> matched = this.processorTable.get(msg.fixedHeader().messageType());
        final Pair<BrokerProcess, ExecutorService> pair = null == matched ? this.defaultRequestProcessor : matched;

        if (pair != null) {
            Runnable run = new Runnable() {
                @Override
                public void run() {
                    pair.getObject1().processRequest(ctx, msg);
                }
            };
            final RequestTask task = new RequestTask(run, ctx.channel(), msg);
            pair.getObject2().submit(task);
        }
    }

}

