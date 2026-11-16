package com.example.saccoauthservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaccoAuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaccoAuthServiceApplication.class, args);
    }

}
