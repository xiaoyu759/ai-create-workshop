package com.aicreative.aicreativeuser;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@MapperScan("com.aicreative.aicreativeuser.mapper")
@ComponentScan("com.aicreative")
public class AiCreativeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiCreativeUserApplication.class, args);
    }
}
