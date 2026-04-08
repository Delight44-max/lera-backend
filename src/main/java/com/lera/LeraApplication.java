package com.lera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LeraApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeraApplication.class, args);
    }
}
