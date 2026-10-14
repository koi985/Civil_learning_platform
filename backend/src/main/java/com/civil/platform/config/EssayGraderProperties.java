package com.civil.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.essay-grader")
public class EssayGraderProperties {

    private String apiUrl;

    private String botId;

    private String apiKey;
}
