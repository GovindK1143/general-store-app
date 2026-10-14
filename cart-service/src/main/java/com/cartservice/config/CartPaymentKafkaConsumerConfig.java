
package com.cartservice.config;

import java.util.HashMap;
import java.util.Map;

import com.cartservice.dto.CartPaymentStatusMessage;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
@EnableKafka
public class CartPaymentKafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, CartPaymentStatusMessage>
    cartPaymentStatusConsumerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                "cart-payment-status-group"
        );

        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "latest"
        );

        JsonDeserializer<CartPaymentStatusMessage> valueDeserializer =
                new JsonDeserializer<>(CartPaymentStatusMessage.class);

        // Use our Cart Service DTO instead of the Java class
        // named in Payment Service's Kafka type header.
        valueDeserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean(name = "cartPaymentStatusKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<
            String, CartPaymentStatusMessage>
    cartPaymentStatusKafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<
                String, CartPaymentStatusMessage> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(
                cartPaymentStatusConsumerFactory()
        );

        return factory;
    }
}
