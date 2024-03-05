package org.eu.polarexpress.conductor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "discord")
@Data
public class DiscordConfig {
    private String prefix;
    private String token;
    private String deepl;
}
