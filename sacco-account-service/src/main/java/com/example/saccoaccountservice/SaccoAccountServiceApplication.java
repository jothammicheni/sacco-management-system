package com.example.saccoaccountservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaccoAccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaccoAccountServiceApplication.class, args);
    }

}
