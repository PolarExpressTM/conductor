package org.eu.polarexpress.conductor.config

import lombok.Data
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "discord")
@Data
class DiscordConfig {
    private val token: String? = null
}
