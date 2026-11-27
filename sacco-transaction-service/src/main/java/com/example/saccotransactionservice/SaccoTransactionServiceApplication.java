package com.example.saccotransactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaccoTransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaccoTransactionServiceApplication.class, args);
    }

}
