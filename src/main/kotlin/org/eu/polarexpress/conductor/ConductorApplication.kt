package org.eu.polarexpress.conductor

import org.eu.polarexpress.conductor.config.DiscordConfig
import org.eu.polarexpress.conductor.discord.DiscordBot
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication


@SpringBootApplication
@EnableConfigurationProperties(DiscordConfig::class)
class ConductorApplication

@Autowired
private val discordBot: DiscordBot? = null

fun main(args: Array<String>) {
	runApplication<ConductorApplication>(*args)
}
