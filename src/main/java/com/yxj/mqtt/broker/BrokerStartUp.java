package com.yxj.mqtt.broker;

import com.yxj.mqtt.config.BrokerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class BrokerStartUp {
    @Autowired
    private BrokerProperties brokerProperties;

    @PostConstruct
    public void startUp() {
        final BrokerController controller = new BrokerController(brokerProperties);
        controller.initBroker();
        controller.startBroker();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                controller.shutdownBroker();
            }
        });
    }
}
