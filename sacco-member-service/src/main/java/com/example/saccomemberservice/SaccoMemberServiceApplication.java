package com.example.saccomemberservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SaccoMemberServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaccoMemberServiceApplication.class, args);
    }

}
