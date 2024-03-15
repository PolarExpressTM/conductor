package org.eu.polarexpress.conductor.service;

import discord4j.core.object.entity.Guild;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.model.Server;
import org.eu.polarexpress.conductor.repository.ServerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class DiscordService {
    private final ServerRepository serverRepository;

    public Optional<Server> save(Guild guild) {

        return Optional.empty();
    }

}
