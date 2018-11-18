package com.yxj.mqtt.protocol;

import com.yxj.mqtt.store.ISessionStore;
import com.yxj.mqtt.store.impl.SessionStore;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

    /**
     * mqtt协议操作按照网址https://mcxiaoke.gitbooks.io/mqtt-cn/content/mqtt/0301-CONNECT.html
     * 描述的协议进行操作
     */
    public class MqttProtocol {
        private static final Logger LOGGER = LoggerFactory.getLogger(MqttProtocol.class);
        private static MqttProtocol mqttProtocol;

        private ISessionStore sessionStore;

        public MqttProtocol() {
            this.sessionStore = new SessionStore();
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
                // 4、清理会话
                if (mqttMessage.payload().clientIdentifier() == null || mqttMessage.payload().clientIdentifier().length() == 0) {
                    // 如果客户端的id为空，并且clean session为1，服务端需要生成clientid。如果clean session不为1则断开连接
                    if (!mqttMessage.variableHeader().isCleanSession()) {
                        MqttConnAckMessage mqttConnAckMessage = (MqttConnAckMessage) MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false), null);
                        channel.writeAndFlush(mqttConnAckMessage);
                        channel.close();
                        return;
                    }
                }
                // 5、遗嘱标识
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
                if (sessionStore.containsKey(mqttMessage.payload().clientIdentifier())) {

                }
            } catch (Exception e) {
                LOGGER.error("发布消息错误：", e);
            }
        }

        public void connAck(Channel ctx, MqttMessage mqttMessage) {

        }

        public void publish(Channel ctx, MqttMessage mqttMessage) {

        }

        public void pubAck(Channel ctx, MqttMessage mqttMessage) {}

        public void pubRec(Channel ctx, MqttMessage mqttMessage) {}

        public void pubRel(Channel ctx, MqttMessage mqttMessage) {}

        public void pubComp(Channel ctx, MqttMessage mqttMessage) {}

        public void subscribe(Channel ctx, MqttMessage mqttMessage) {}

        public void subAck(Channel ctx, MqttMessage mqttMessage) {}

        public void unsubscribe(Channel ctx, MqttMessage mqttMessage) {}

        public void unsubAck(Channel ctx, MqttMessage mqttMessage) {}

        public void pingReq(Channel ctx, MqttMessage mqttMessage) {}

        public void pingResp(Channel ctx, MqttMessage mqttMessage) {}

        public void disconnect(Channel ctx, MqttMessage mqttMessage) {}


    }
