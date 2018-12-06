package com.yxj.mqtt.protocol;

import com.yxj.mqtt.bean.*;
import com.yxj.mqtt.store.*;
import com.yxj.mqtt.store.impl.*;
import com.yxj.mqtt.utils.StrUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * mqtt协议操作按照网址https://mcxiaoke.gitbooks.io/mqtt-cn/content/mqtt/0301-CONNECT.html
 * 描述的协议进行操作
 */
public class MqttProtocol {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttProtocol.class);
    private static MqttProtocol mqttProtocol;

    private SessionStoreService sessionStoreService;
    private SubscribeMessageStoreService subscribeMessageStoreService;
    private PublishMessageStoreService publishMessageStoreService;
    private PubRelMessageStoreService pubRelMessageStoreService;
    private MessageIdService messageIdService;
    private RetainMessageStoreService retainMessageStoreService;

    public MqttProtocol() {
        this.sessionStoreService = new SessionStoreServiceImpl();
        this.subscribeMessageStoreService = new SubscribeMessageStoreServiceImpl();
        this.publishMessageStoreService = new PublishMessageStoreServiceImpl();
        this.pubRelMessageStoreService = new PubRelMessageStoreServiceImpl();
        this.messageIdService = new MessageIdServiceImpl();
        retainMessageStoreService = new RetainMessageStoreServiceImpl();
    }


    public static MqttProtocol getInstance() {
        if (mqttProtocol == null) {
            mqttProtocol = new MqttProtocol();
        }
        return mqttProtocol;
    }

    /**
     * 客户端连接
     * @param channel
     * @param mqttMessage
     */
    public void connect(Channel channel, MqttConnectMessage mqttMessage) {
        try {
            // 1、判断协议名称是否为MQTT
            if (!mqttMessage.variableHeader().name().equalsIgnoreCase("mqtt")) {
                channel.close();
                return;
            }
            /*
                MqttFixedHeader介绍
                1、消息类型
                2、用来在保证消息的可靠传输，如果设置为1，则在下面的变长中增加MessageId，并且需要回复确认，以保证消息传输完成，但不能用于检测消息重复发送。
                3、消息等级 最多一次、至少一次、刚好一次
                4、如果客户端发送的retain标识为1，则服务端必须保存该条消息以及它的QoS。以便这个topic有新的订阅者订阅时，服务端要把这个消息推送给它。使用Retain的好处就是新的订阅者订阅成功之后便能得到最近的一条消息，无需等到下次产生消息时。每个retain 消息都会覆盖上一条，把这条消息最为最新保留消息。如果服务器收到发送retain为true，payload为空的消息，它会把这个topic保留的retain消息删除。
             */
            // 2、对于3.1.1版协议，协议级别字段的值是4(0x04)。如果发现不支持的协议级别，服务端必须给发送一个返回码为0x01（不支持的协议级别）的CONNACK报文响应CONNECT报文，然后断开客户端的连接
            if (mqttMessage.variableHeader().version() != 4) {
                MqttConnAckMessage mqttConnAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false), null);
                channel.writeAndFlush(mqttConnAckMessage);
                channel.close();
                return;
            }
            // 3、服务端必须验证CONNECT控制报文的保留标志位（第0位）是否为0，如果不为0必须断开客户端连接
            String clientId = mqttMessage.payload().clientIdentifier();
            // 4、清理会话
            if (clientId == null || clientId.length() == 0) {
                // 如果客户端的id为空，并且clean session为1，服务端需要生成clientid。如果clean session不为1则断开连接
//                if (!mqttMessage.variableHeader().isCleanSession()) {
                MqttConnAckMessage mqttConnAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false), null);
                channel.writeAndFlush(mqttConnAckMessage);
                channel.close();
                return;
//                }
            }
            // 断开连接
            if (sessionStoreService.containsKey(clientId)) {
                Session session = sessionStoreService.get(clientId);
                Channel oldChannel = session.getChannel();
                if (session.isCleanSession()) {
                    sessionStoreService.remove(clientId);
                    publishMessageStoreService.removeByClient(clientId);
                    pubRelMessageStoreService.removeByClient(clientId);
                    subscribeMessageStoreService.removeForClient(clientId);
                }
                oldChannel.close();
            }
            Session session = new Session(clientId, channel, mqttMessage.variableHeader().isCleanSession(), null);
            // 5、遗嘱标识，保存遗嘱信息
            if (mqttMessage.variableHeader().isWillFlag()) {
                MqttPublishMessage willMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.valueOf(mqttMessage.variableHeader().willQos()), mqttMessage.variableHeader().isWillRetain(), 0),
                        new MqttPublishVariableHeader(mqttMessage.payload().willTopic(), 0), Unpooled.buffer().writeBytes(mqttMessage.payload().willMessageInBytes())
                );
                session.setWillMessage(willMessage);
            }
            // 6、遗嘱QOS
            // 7、遗嘱保留
            // 8、用户标志 9、密码标识  如果设置了用户名和密码就需要对其验证，没有设置不做处理
            if (mqttMessage.variableHeader().hasPassword() && mqttMessage.variableHeader().hasUserName()) {
                String userName = mqttMessage.payload().userName();
                String password = mqttMessage.payload().password();
                if (!userName.equalsIgnoreCase("yxj") || !password.equalsIgnoreCase("123")) {
                    MqttConnAckMessage mqttConnAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false), null);
                    channel.writeAndFlush(mqttConnAckMessage);
                    channel.close();
                    return;
                }
            }
            // 10、保持连接
            if (mqttMessage.variableHeader().keepAliveTimeSeconds() > 0) {
                if (channel.pipeline().names().contains("idle")) {
                    channel.pipeline().remove("idle");
                }
                channel.pipeline().addFirst("idle", new IdleStateHandler(0,0,Math.round(1.5f*mqttMessage.variableHeader().keepAliveTimeSeconds())));
            }
            sessionStoreService.put(clientId, session);
            // 将clientId放到Channel中，下次用户发送数据时，可以在channel中获取clientId
            channel.attr(AttributeKey.valueOf("clientId")).set(clientId);
            // 返回连接成功
            boolean sessionPresent = sessionStoreService.containsKey(clientId) && !mqttMessage.variableHeader().isCleanSession();
            MqttConnAckMessage ackMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent), null
            );
            channel.writeAndFlush(ackMessage);
            // 如果cleanSession为0，需要为重连的client发送之前存储的QOS1和QoS2的消息
            if (!mqttMessage.variableHeader().isCleanSession()) {
                List<PublishMessage> qoS1Messages = publishMessageStoreService.get(clientId);
                List<PubRelMessage> qoS2Messages = pubRelMessageStoreService.get(clientId);
                qoS1Messages.forEach(message ->{
                    MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBLISH, true, MqttQoS.valueOf(message.getMqttQoS()), false, 0),
                            new MqttPublishVariableHeader(message.getTopic(), message.getMessageId()), Unpooled.buffer().writeBytes(message.getMessageBytes())
                    );
                    channel.writeAndFlush(publishMessage);
                });
                qoS2Messages.forEach(message->{
                    MqttMessage publishMessage = (MqttMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBREL, true, MqttQoS.AT_MOST_ONCE, false, 0),
                            MqttMessageIdVariableHeader.from(message.getMessageId()), null
                    );
                    channel.writeAndFlush(publishMessage);
                });
            }
        } catch (Exception e) {
            LOGGER.error("发布消息错误：", e);
        }
    }

    public void publish(Channel ctx, MqttPublishMessage mqttMessage) {
        // 发送消息给所有的客户端即可
        if (mqttMessage.fixedHeader().qosLevel() == MqttQoS.AT_MOST_ONCE) {
            int num = mqttMessage.payload().readableBytes();
            byte[] messageBytes = new byte[num];
            mqttMessage.payload().getBytes(mqttMessage.payload().readerIndex(), messageBytes);
            sendPublishMessage(mqttMessage.variableHeader().topicName(), mqttMessage.fixedHeader().qosLevel(), messageBytes, false, false)  ;
        }

        if (mqttMessage.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE) {
            byte[] messageBytes = new byte[mqttMessage.payload().readableBytes()];
            mqttMessage.payload().getBytes(mqttMessage.payload().readerIndex(), messageBytes);
            sendPublishMessage(mqttMessage.variableHeader().topicName(), mqttMessage.fixedHeader().qosLevel(), messageBytes, false, false)  ;
            sendPubAckMessage(ctx, mqttMessage.variableHeader().packetId());
        }

        if (mqttMessage.fixedHeader().qosLevel() == MqttQoS.EXACTLY_ONCE) {
            byte[] messageBytes = new byte[mqttMessage.payload().readableBytes()];
            mqttMessage.payload().getBytes(mqttMessage.payload().readerIndex(), messageBytes);
            sendPublishMessage(mqttMessage.variableHeader().topicName(), mqttMessage.fixedHeader().qosLevel(), messageBytes, false, false)  ;
            sendPucRecMessage(ctx, mqttMessage.variableHeader().packetId());
        }

        if (mqttMessage.fixedHeader().isRetain()) {

        }
    }

    public void pubAck(Channel ctx, MqttMessageIdVariableHeader variableHeader) {
        int messageId = variableHeader.messageId();
        LOGGER.debug("PUBACK - clientId: {}, messageId: {}", (String) ctx.attr(AttributeKey.valueOf("clientId")).get(), messageId);
        publishMessageStoreService.remove((String) ctx.attr(AttributeKey.valueOf("clientId")).get(), messageId);
        messageIdService.releaseMessageId(messageId);
    }

    public void pubRec(Channel ctx, MqttMessageIdVariableHeader variableHeader) {
        MqttMessage pubRelMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(variableHeader.messageId()), null);
        LOGGER.debug("PUBREC - clientId: {}, messageId: {}", (String) ctx.attr(AttributeKey.valueOf("clientId")).get(), variableHeader.messageId());
        publishMessageStoreService.remove((String) ctx.attr(AttributeKey.valueOf("clientId")).get(), variableHeader.messageId());
        PublishMessage dupPubRelMessageStore = new PublishMessage().setClientId((String) ctx.attr(AttributeKey.valueOf("clientId")).get())
                .setMessageId(variableHeader.messageId());
        publishMessageStoreService.put((String) ctx.attr(AttributeKey.valueOf("clientId")).get(), dupPubRelMessageStore);
        ctx.writeAndFlush(pubRelMessage);
    }

    public void pubRel(Channel ctx, MqttMessageIdVariableHeader variableHeader) {
        MqttMessage pubCompMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(variableHeader.messageId()), null);
        LOGGER.debug("PUBREL - clientId: {}, messageId: {}", (String) ctx.attr(AttributeKey.valueOf("clientId")).get(), variableHeader.messageId());
        ctx.writeAndFlush(pubCompMessage);
    }

    public void pubComp(Channel ctx, MqttMessageIdVariableHeader variableHeader) {
        int messageId = variableHeader.messageId();
        LOGGER.debug("PUBCOMP - clientId: {}, messageId: {}", (String) ctx.attr(AttributeKey.valueOf("clientId")).get(), messageId);
        publishMessageStoreService.remove((String) ctx.attr(AttributeKey.valueOf("clientId")).get(), variableHeader.messageId());
        messageIdService.releaseMessageId(messageId);
    }

    public void subscribe(Channel ctx, MqttSubscribeMessage mqttMessage) {
        List<MqttTopicSubscription> topicSubscriptions = mqttMessage.payload().topicSubscriptions();
        if (this.validTopicFilter(topicSubscriptions)) {
            String clientId = (String) ctx.attr(AttributeKey.valueOf("clientId")).get();
            List<Integer> mqttQoSList = new ArrayList<Integer>();
            topicSubscriptions.forEach(topicSubscription -> {
                String topicFilter = topicSubscription.topicName();
                MqttQoS mqttQoS = topicSubscription.qualityOfService();
                SubscribeMessage subscribeStore = new SubscribeMessage(clientId, topicFilter, mqttQoS.value());
                subscribeMessageStoreService.put(topicFilter, subscribeStore);
                mqttQoSList.add(mqttQoS.value());
                LOGGER.debug("SUBSCRIBE - clientId: {}, topFilter: {}, QoS: {}", clientId, topicFilter, mqttQoS.value());
            });
            MqttSubAckMessage subAckMessage = (MqttSubAckMessage) MqttMessageFactory.newMessage(
                    new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().messageId()),
                    new MqttSubAckPayload(mqttQoSList));
            ctx.writeAndFlush(subAckMessage);
            // 发布保留消息
            topicSubscriptions.forEach(topicSubscription -> {
                String topicFilter = topicSubscription.topicName();
                MqttQoS mqttQoS = topicSubscription.qualityOfService();
                this.sendRetainMessage(ctx, topicFilter, mqttQoS);
            });
        } else {
            ctx.close();
        }
    }

    public void unsubscribe(Channel ctx, MqttUnsubscribeMessage mqttMessage) {
        List<String> topicFilters = mqttMessage.payload().topics();
        String clinetId = (String) ctx.attr(AttributeKey.valueOf("clientId")).get();
        topicFilters.forEach(topicFilter -> {
            subscribeMessageStoreService.remove(topicFilter, clinetId);
            LOGGER.debug("UNSUBSCRIBE - clientId: {}, topicFilter: {}", clinetId, topicFilter);
        });
        MqttUnsubAckMessage unsubAckMessage = (MqttUnsubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(mqttMessage.variableHeader().messageId()), null);
        ctx.writeAndFlush(unsubAckMessage);
    }

    public void pingReq(Channel ctx, MqttMessage mqttMessage) {
        MqttMessage pingRespMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0), null, null);
        LOGGER.debug("PINGREQ - clientId: {}", (String) ctx.attr(AttributeKey.valueOf("clientId")).get());
        ctx.writeAndFlush(pingRespMessage);
    }

    public void disconnect(Channel ctx, MqttMessage mqttMessage) {
        String clientId = (String) ctx.attr(AttributeKey.valueOf("clientId")).get();
        Session session = sessionStoreService.get(clientId);
        if (session.isCleanSession()) {
            publishMessageStoreService.removeByClient(clientId);
            pubRelMessageStoreService.removeByClient(clientId);
            subscribeMessageStoreService.removeForClient(clientId);
        }
        sessionStoreService.remove(clientId);
        ctx.close();
    }

    private void sendPublishMessage(String topicName, MqttQoS qosLevel, byte[] messageBytes, boolean retain, boolean dup) {
        // 查询订阅了该topic的所有用户信息
        List<SubscribeMessage> subscribeStores = subscribeMessageStoreService.search(topicName);
        subscribeStores.forEach(subscribeStore -> {
            if (sessionStoreService.containsKey(subscribeStore.getClientId())) {
                MqttQoS resQos = qosLevel.value() > subscribeStore.getMqttQoS() ? MqttQoS.valueOf(subscribeStore.getMqttQoS()) : qosLevel;
                if (resQos == MqttQoS.AT_MOST_ONCE) {
                    MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBLISH, dup, resQos, retain, 0),
                            new MqttPublishVariableHeader(topicName, 0), Unpooled.buffer().writeBytes(messageBytes));
                    sessionStoreService.get(subscribeStore.getClientId()).getChannel().writeAndFlush(publishMessage);
                }

                if (resQos == MqttQoS.AT_LEAST_ONCE) {
                    int messageId = messageIdService.getNextMessageId();
                    MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBLISH, dup, resQos, retain, 0),
                            new MqttPublishVariableHeader(topicName, messageId), Unpooled.buffer().writeBytes(messageBytes));
                    LOGGER.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}, messageId: {}", subscribeStore.getClientId(), topicName, resQos.value(), messageId);
                    PublishMessage dupPublishMessageStore = new PublishMessage().setClientId(subscribeStore.getClientId())
                            .setTopic(topicName).setMqttQoS(resQos.value()).setMessageBytes(messageBytes);
                    publishMessageStoreService.put(subscribeStore.getClientId(), dupPublishMessageStore);
                    sessionStoreService.get(subscribeStore.getClientId()).getChannel().writeAndFlush(publishMessage);
                }

                if (resQos == MqttQoS.EXACTLY_ONCE) {
                    int messageId = messageIdService.getNextMessageId();
                    MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.PUBLISH, dup, resQos, retain, 0),
                            new MqttPublishVariableHeader(topicName, messageId), Unpooled.buffer().writeBytes(messageBytes));
                    LOGGER.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}, messageId: {}", subscribeStore.getClientId(), topicName, resQos.value(), messageId);
                    PublishMessage dupPublishMessageStore = new PublishMessage().setClientId(subscribeStore.getClientId())
                            .setTopic(topicName).setMqttQoS(resQos.value()).setMessageBytes(messageBytes);
                    publishMessageStoreService.put(subscribeStore.getClientId(), dupPublishMessageStore);
                    sessionStoreService.get(subscribeStore.getClientId()).getChannel().writeAndFlush(publishMessage);
                }
            }
        });
    }

    private void sendPubAckMessage(Channel channel, int messageId) {
        MqttPubAckMessage pubAckMessage = (MqttPubAckMessage) MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), null);
        channel.writeAndFlush(pubAckMessage);
    }

    private void sendPucRecMessage(Channel channel, int messageId) {
        MqttMessage pubRecMessage = MqttMessageFactory.newMessage(
                new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0),
                MqttMessageIdVariableHeader.from(messageId), null);
        channel.writeAndFlush(pubRecMessage);
    }

    private boolean validTopicFilter(List<MqttTopicSubscription> topicSubscriptions) {
        for (MqttTopicSubscription topicSubscription : topicSubscriptions) {
            String topicFilter = topicSubscription.topicName();
            // 以#或+符号开头的、以/符号结尾的及不存在/符号的订阅按非法订阅处理, 这里没有参考标准协议
            if (StrUtil.startWith(topicFilter, '#') || StrUtil.startWith(topicFilter, '+') || StrUtil.endWith(topicFilter, '/') || !StrUtil.contains(topicFilter, '/')) return false;
            if (StrUtil.contains(topicFilter, '#')) {
                // 不是以/#字符串结尾的订阅按非法订阅处理
                if (!StrUtil.endWith(topicFilter, "/#")) return false;
                // 如果出现多个#符号的订阅按非法订阅处理
                if (StrUtil.count(topicFilter, '#') > 1) return false;
            }
            if (StrUtil.contains(topicFilter, '+')) {
                //如果+符号和/+字符串出现的次数不等的情况按非法订阅处理
                if (StrUtil.count(topicFilter, '+') != StrUtil.count(topicFilter, "/+")) return false;
            }
        }
        return true;
    }

    private void sendRetainMessage(Channel channel, String topicFilter, MqttQoS mqttQoS) {
        List<RetainMessage> retainMessageStores = retainMessageStoreService.search(topicFilter);
        retainMessageStores.forEach(retainMessageStore -> {
            MqttQoS respQoS = retainMessageStore.getMqttQoS() > mqttQoS.value() ? mqttQoS : MqttQoS.valueOf(retainMessageStore.getMqttQoS());
            if (respQoS == MqttQoS.AT_MOST_ONCE) {
                MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, respQoS, false, 0),
                        new MqttPublishVariableHeader(retainMessageStore.getTopic(), 0), Unpooled.buffer().writeBytes(retainMessageStore.getMessageBytes()));
                LOGGER.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}", (String) channel.attr(AttributeKey.valueOf("clientId")).get(), retainMessageStore.getTopic(), respQoS.value());
                channel.writeAndFlush(publishMessage);
            }
            if (respQoS == MqttQoS.AT_LEAST_ONCE) {
                int messageId = messageIdService.getNextMessageId();
                MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, respQoS, false, 0),
                        new MqttPublishVariableHeader(retainMessageStore.getTopic(), messageId), Unpooled.buffer().writeBytes(retainMessageStore.getMessageBytes()));
                LOGGER.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}, messageId: {}", (String) channel.attr(AttributeKey.valueOf("clientId")).get(), retainMessageStore.getTopic(), respQoS.value(), messageId);
                channel.writeAndFlush(publishMessage);
            }
            if (respQoS == MqttQoS.EXACTLY_ONCE) {
                int messageId = messageIdService.getNextMessageId();
                MqttPublishMessage publishMessage = (MqttPublishMessage) MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, respQoS, false, 0),
                        new MqttPublishVariableHeader(retainMessageStore.getTopic(), messageId), Unpooled.buffer().writeBytes(retainMessageStore.getMessageBytes()));
                LOGGER.debug("PUBLISH - clientId: {}, topic: {}, Qos: {}, messageId: {}", (String) channel.attr(AttributeKey.valueOf("clientId")).get(), retainMessageStore.getTopic(), respQoS.value(), messageId);
                channel.writeAndFlush(publishMessage);
            }
        });
    }


}