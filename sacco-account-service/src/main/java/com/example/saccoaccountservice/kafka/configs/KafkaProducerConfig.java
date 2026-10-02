package com.example.saccoaccountservice.kafka.configs;

import com.example.saccoevents.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {
    @Bean
    public NewTopic AccountCreatedTopics(){
        return TopicBuilder.name(Topics.ACCOUNT_CREATED)
                .replicas(1)
                .partitions(3)
                .build();

    }

    @Bean
    public NewTopic AccountCreditedTopics(){
        return TopicBuilder.name(Topics.ACCOUNT_CREDITED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic AccountFrozenTopics(){
        return TopicBuilder.name(Topics.ACCOUNT_FROZEN)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic AccountDebitedTopics(){
        return TopicBuilder.name(Topics.ACCOUNT_DEBITED)
                .partitions(3)
                .replicas(1)
                .build();
    }




}
