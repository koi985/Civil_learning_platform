package com.civil.platform;

import com.civil.platform.config.AiAssistantProperties;
import com.civil.platform.config.EssayGraderProperties;
import com.civil.platform.config.FileStorageProperties;
import com.civil.platform.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, FileStorageProperties.class, AiAssistantProperties.class, EssayGraderProperties.class})
public class CivilLearningApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivilLearningApplication.class, args);
    }
}
