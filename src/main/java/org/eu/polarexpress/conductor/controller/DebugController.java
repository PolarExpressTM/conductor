package org.eu.polarexpress.conductor.controller;

import discord4j.common.util.Snowflake;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.eu.polarexpress.conductor.model.Server;
import org.eu.polarexpress.conductor.service.DiscordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class DebugController {
    private final DiscordBot discordBot;
    private final DiscordService discordService;

    @GetMapping("/changeNickname/{nickname}")
    public ResponseEntity<String> changeNickname(@PathVariable String nickname) {
        discordBot.getClient()
                .getGuildById(Snowflake.of("992115669774635078"))
                .map(guild -> guild.changeSelfNickname(nickname))
                .subscribe();
        return ResponseEntity.ok("Changed nickname!");
    }

    @GetMapping("/servers")
    public ResponseEntity<List<String>> getServers() {
        return ResponseEntity.ok(discordService.findAllServers().stream().map(Server::toString).toList());
    }

}
