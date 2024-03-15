package org.eu.polarexpress.conductor.discord;

import discord4j.core.object.entity.Guild;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class DiscordModelHandler {

    public Guild saveGuild(Guild guild) {
        guild.getChannels()
                .map(guildChannel -> guildChannel)
                .subscribe();
        return null;
    }

}
