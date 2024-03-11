package org.eu.polarexpress.conductor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pixiv")
@Data
public class PixivConfig {
    private String session;
    private int maxPages;
}
