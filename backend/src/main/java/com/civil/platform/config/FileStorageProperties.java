package com.civil.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.file")
public class FileStorageProperties {

    private String basePath;

    private String publicPrefix;
}
