package com.yxj.mqtt.process.impl;

import com.yxj.mqtt.process.BrokerProcess;
import com.yxj.mqtt.protocol.MqttProtocol;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.mqtt.*;

public class DefaultBrokerProcess implements BrokerProcess {

    MqttProtocol mqttProtocol = MqttProtocol.getInstance();

    @Override
    public void processRequest(ChannelHandlerContext channelHandlerContext, MqttMessage mqttMessage) {
        Channel channel = channelHandlerContext.channel();
        switch (mqttMessage.fixedHeader().messageType()) {
            case CONNECT:
                mqttProtocol.connect(channel, (MqttConnectMessage) mqttMessage);
                break;
            case PUBLISH:
                mqttProtocol.publish(channel, (MqttPublishMessage) mqttMessage);
                ((MqttPublishMessage) mqttMessage).payload().release();
                break;
            case PUBACK:
                mqttProtocol.pubAck(channel, (MqttMessageIdVariableHeader) mqttMessage.variableHeader());
                break;
            case PUBREC:
                mqttProtocol.pubRec(channel, (MqttMessageIdVariableHeader) mqttMessage.variableHeader());
                break;
            case PUBREL:
                mqttProtocol.pubRel(channel, (MqttMessageIdVariableHeader) mqttMessage.variableHeader());
                break;
            case PUBCOMP:
                mqttProtocol.pubComp(channel, (MqttMessageIdVariableHeader) mqttMessage.variableHeader());
                break;
            case SUBSCRIBE:
                mqttProtocol.subscribe(channel, (MqttSubscribeMessage) mqttMessage);
                break;
            case SUBACK:
//                mqttProtocol.subAck(channel, mqttMessage);
                break;
            case UNSUBSCRIBE:
                mqttProtocol.unsubscribe(channel, (MqttUnsubscribeMessage) mqttMessage);
                break;
            case UNSUBACK:
//                mqttProtocol.unsubAck(channel, mqttMessage);
                break;
            case PINGREQ:
                mqttProtocol.pingReq(channel, mqttMessage);
                break;
            case PINGRESP:
//                mqttProtocol.pingResp(channel, mqttMessage);
                break;
            case DISCONNECT:
                mqttProtocol.disconnect(channel, mqttMessage);
                break;
            default:
                break;
        }

    }
}
