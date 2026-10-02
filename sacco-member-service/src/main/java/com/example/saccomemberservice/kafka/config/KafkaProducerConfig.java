package com.example.saccomemberservice.kafka.config;


import com.example.saccoevents.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public NewTopic memberCreatedTopics(){
        return TopicBuilder.name(Topics.MEMBER_CREATED)
                .replicas(1)
                .partitions(3)
                .build();
    }
    @Bean
    public NewTopic memberDeletedTopics(){
        return TopicBuilder.name(Topics.MEMBER_DELETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic memberUpdatedTopic(){
        return TopicBuilder.name(Topics.MEMBER_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic memberSuspendedTopic(){
        return TopicBuilder.name(Topics.MEMBER_SUSPENDED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
