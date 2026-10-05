package com.paymentservice.config;

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

import com.paymentservice.model.PaymentStatusMessage;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, PaymentStatusMessage> producerFactory() {

        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );

        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );

        // =====================================================
        // KAFKA PRODUCER RELIABILITY
        // =====================================================

        /*
         * Wait for acknowledgement from all in-sync replicas.
         * This provides stronger durability than acks=1.
         */
        config.put(
                ProducerConfig.ACKS_CONFIG,
                "all"
        );

        /*
         * Enable Kafka producer idempotence.
         *
         * Prevents duplicate records caused by producer retries.
         */
        config.put(
                ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,
                true
        );

        /*
         * Allow the producer to retry transient failures.
         *
         * With idempotence enabled, Kafka safely handles
         * producer retries without creating duplicate records.
         */
        config.put(
                ProducerConfig.RETRIES_CONFIG,
                Integer.MAX_VALUE
        );

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, PaymentStatusMessage> kafkaTemplate() {

        return new KafkaTemplate<>(producerFactory());
    }
}