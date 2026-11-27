package com.example.saccotransactionservice.kafka.config;


import com.example.saccoevents.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.internals.Topic;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public NewTopic TransactionCreatedTopic(){
        return TopicBuilder.name(Topics.TRANSACTION_CREATED)
                .replicas(1)
                .partitions(3)
                .build();
    }

}
