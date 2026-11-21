package com.aicreative.aicreativeworkshop;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubbo
public class AiCreativeScreenshotApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCreativeScreenshotApplication.class,args);
    }
}
