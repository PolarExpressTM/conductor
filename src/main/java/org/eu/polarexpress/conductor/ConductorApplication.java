package org.eu.polarexpress.conductor;

import org.eu.polarexpress.conductor.config.DiscordConfig;
import org.eu.polarexpress.conductor.config.PixivConfig;
import org.eu.polarexpress.conductor.config.UtilConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        DiscordConfig.class,
        PixivConfig.class,
        UtilConfig.class
})
public class ConductorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConductorApplication.class, args);
    }

}
