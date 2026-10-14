package com.civil.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ai-assistant")
public class AiAssistantProperties {

    private String apiUrl;

    private String botId;

    private String apiKey;
}
