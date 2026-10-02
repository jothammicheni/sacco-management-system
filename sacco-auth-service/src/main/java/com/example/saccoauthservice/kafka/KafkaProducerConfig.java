package com.example.saccoauthservice.kafka;

import com.example.saccoevents.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaProducerConfig {

    @Bean
    public NewTopic userOtpRequestedTopic() {
        return TopicBuilder.name(Topics.USER_OTP_REQUESTED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userActivatedTopic() {
        return TopicBuilder.name(Topics.USER_ACTIVATED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}