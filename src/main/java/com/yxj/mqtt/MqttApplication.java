package com.yxj.mqtt;

import com.yxj.mqtt.config.BrokerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = "com.yxj.mqtt.broker")
public class MqttApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttApplication.class, args);
    }
    @Bean
    public BrokerProperties brokerPropertes(){
        return new BrokerProperties();
    }
}
