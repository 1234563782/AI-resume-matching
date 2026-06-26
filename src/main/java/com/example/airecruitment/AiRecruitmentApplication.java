package com.example.airecruitment;

import com.example.airecruitment.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class AiRecruitmentApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiRecruitmentApplication.class, args);
    }
}
