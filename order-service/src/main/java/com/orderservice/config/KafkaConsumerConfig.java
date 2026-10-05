package com.orderservice.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.kafka.annotation.EnableKafka;

import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import org.springframework.kafka.core.KafkaTemplate;

import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import org.springframework.kafka.support.serializer.JsonDeserializer;

import org.springframework.util.backoff.FixedBackOff;

import com.orderservice.model.PaymentStatusMessage;


@Configuration
@EnableKafka
public class KafkaConsumerConfig {


    // =========================================================
    // KAFKA BOOTSTRAP SERVERS
    // =========================================================

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;


    // =========================================================
    // KAFKA CONSUMER GROUP
    // =========================================================

    @Value("${spring.kafka.consumer.group-id:order-group}")
    private String groupId;


    // =========================================================
    // KAFKA TEMPLATE
    //
    // KafkaConfig already creates the KafkaTemplate bean.
    // We inject and reuse that bean here for DLT publishing.
    // =========================================================

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;


    // =========================================================
    // CONSUMER FACTORY
    // =========================================================

    @Bean
    public ConsumerFactory<String, PaymentStatusMessage>
    consumerFactory() {


        JsonDeserializer<PaymentStatusMessage>
                deserializer =
                new JsonDeserializer<>(
                        PaymentStatusMessage.class
                );


        deserializer.setRemoveTypeHeaders(
                false
        );


        deserializer.addTrustedPackages(
                "com.orderservice.model"
        );


        deserializer.setUseTypeMapperForKey(
                true
        );


        Map<String, Object> props =
                new HashMap<>();


        props.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );


        props.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );


        props.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );


        props.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );


        /*
         * If a new consumer group starts and there are
         * existing messages, consume from the earliest
         * available offset.
         */
        props.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );


        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer
        );
    }


    // =========================================================
    // NORMAL KAFKA LISTENER CONTAINER FACTORY
    // =========================================================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String,
            PaymentStatusMessage>
    kafkaListenerContainerFactory() {


        ConcurrentKafkaListenerContainerFactory<
                String,
                PaymentStatusMessage>
                factory =
                new ConcurrentKafkaListenerContainerFactory<>();


        // -----------------------------------------------------
        // Consumer Factory
        // -----------------------------------------------------

        factory.setConsumerFactory(
                consumerFactory()
        );


        // =====================================================
        // DEAD LETTER PUBLISHING RECOVERER
        // =====================================================
        //
        // If all Kafka-level retries are exhausted,
        // the failed message is published to a DLT.
        //
        // Explicit DLT:
        //
        // payment.status.topic-dlt
        //
        // =====================================================

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(
                                        "payment.status.topic-dlt",
                                        record.partition()
                                )
                );


        // =====================================================
        // KAFKA ERROR HANDLER
        // =====================================================
        //
        // FixedBackOff:
        //
        // 1000L = 1 second between retries
        //
        // 2L = 2 additional retry attempts
        //
        // Therefore:
        //
        // Initial attempt
        //      ↓
        // Retry #1
        //      ↓
        // Retry #2
        //      ↓
        // DLT
        //
        // =====================================================

        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(
                        recoverer,
                        new FixedBackOff(
                                1000L,
                                2L
                        )
                );


        // -----------------------------------------------------
        // Register Kafka Error Handler
        // -----------------------------------------------------

        factory.setCommonErrorHandler(
                errorHandler
        );


        return factory;
    }


    // =========================================================
    // DLT KAFKA LISTENER CONTAINER FACTORY
    // =========================================================
    //
    // This factory is ONLY for the DLT listener.
    //
    // IMPORTANT:
    //
    // We intentionally DO NOT use
    // DeadLetterPublishingRecoverer here.
    //
    // Otherwise, if DLT processing fails, the message could
    // be published back to the same DLT and cause recursive
    // DLT processing.
    //
    // =========================================================

    @Bean
    public ConcurrentKafkaListenerContainerFactory<
            String,
            PaymentStatusMessage>
    dltKafkaListenerContainerFactory() {


        ConcurrentKafkaListenerContainerFactory<
                String,
                PaymentStatusMessage>
                factory =
                new ConcurrentKafkaListenerContainerFactory<>();


        // -----------------------------------------------------
        // Consumer Factory
        // -----------------------------------------------------

        factory.setConsumerFactory(
                consumerFactory()
        );


        // =====================================================
        // DLT ERROR HANDLER
        // =====================================================
        //
        // DLT messages get:
        //
        // Initial attempt
        //      ↓
        // Retry #1 after 1 second
        //      ↓
        // Retry #2 after 1 second
        //      ↓
        // Stop processing
        //
        // There is NO DLT recoverer here.
        //
        // Therefore, a failed DLT message will NOT be
        // published back to payment.status.topic-dlt.
        //
        // =====================================================

        DefaultErrorHandler dltErrorHandler =
                new DefaultErrorHandler(
                        new FixedBackOff(
                                1000L,
                                2L
                        )
                );


        // -----------------------------------------------------
        // Register DLT Error Handler
        // -----------------------------------------------------

        factory.setCommonErrorHandler(
                dltErrorHandler
        );


        return factory;
    }
}