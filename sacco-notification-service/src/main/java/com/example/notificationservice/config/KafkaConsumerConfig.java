package com.example.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler errorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, ex) -> log.error(
                        "Giving up on record topic={} partition={} offset={}: {}",
                        record.topic(), record.partition(), record.offset(),
                        ex.getMessage(), ex),
                new FixedBackOff(2000L, 3L)
        );
        handler.addNotRetryableExceptions(DeserializationException.class);
        return handler;
    }
}