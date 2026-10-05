package com.orderservice.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {

        Map<String, Object> configProps =
                new HashMap<>();

        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );

        // =====================================================
        // KAFKA PRODUCER RELIABILITY
        // =====================================================

        /*
         * Wait for acknowledgement from all in-sync replicas.
         * Provides stronger durability for produced messages.
         */
        configProps.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        /*
         * Enable Kafka producer idempotence.
         *
         * Prevents duplicate records when the producer retries
         * sending a message.
         */
        configProps.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        /*
         * Retry transient Kafka failures.
         *
         * With idempotence enabled, producer retries are
         * handled safely without creating duplicate records.
         */
        configProps.put(
                ProducerConfig.RETRIES_CONFIG,
                Integer.MAX_VALUE
        );

        return new DefaultKafkaProducerFactory<>(
                configProps
        );
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {

        return new KafkaTemplate<>(
                producerFactory()
        );
    }
}