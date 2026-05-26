package com.nullify.supportportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SupportPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportPortalApplication.class, args);
    }
}
