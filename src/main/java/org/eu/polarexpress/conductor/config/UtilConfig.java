package org.eu.polarexpress.conductor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "util")
@Data
public class UtilConfig {
    String userAgent;
}
