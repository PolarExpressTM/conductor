package org.eu.polarexpress.conductor.service;

import discord4j.core.object.entity.Guild;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.model.Server;
import org.eu.polarexpress.conductor.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class DiscordService {
    private final ServerRepository serverRepository;

    public List<Server> findAllServers() {
        return serverRepository.findAll();
    }

    public Server save(Guild guild) {
        return serverRepository.findBySnowflakeId(guild.getId().asString())
                .map(server -> server.merge(guild))
                .orElseGet(() -> serverRepository.save(Server.fromGuild(guild)));
    }

}
