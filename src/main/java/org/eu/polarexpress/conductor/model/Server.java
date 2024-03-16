package org.eu.polarexpress.conductor.model;

import discord4j.core.object.entity.Guild;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString
public class Server extends BaseEntity {
    @NotBlank
    @Setter
    private String snowflakeId;
    @NotBlank
    @Setter
    private String name;
    @Setter
    private String description;
    @OneToMany(orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @Builder.Default
    private List<Channel> channels = new ArrayList<>();

    public List<Channel> getChannels() {
        return List.copyOf(channels);
    }

    public Channel addChannel(Channel channel) {
        if (!channels.contains(channel)) {
            channels.add(channel);
        }
        return channel;
    }

    public Channel removeChannel(Channel channel) {
        channels.remove(channel);
        return channel;
    }

    public Server merge(Guild guild) {
        setName(guild.getName());
        guild.getDescription().ifPresent(this::setDescription);
        getChannels().forEach(this::removeChannel);
        guild.getChannels()
                .map(guildChannel -> {
                    addChannel(Channel.fromChannel(guildChannel));
                    return guildChannel;
                })
                .subscribe();
        return this;
    }

    public static Server fromGuild(Guild guild) {
        var server = Server.builder()
                .snowflakeId(guild.getId().asString())
                .name(guild.getName())
                .description(guild.getDescription().orElse(""))
                .build();
        guild.getChannels()
                .map(guildChannel -> {
                    server.addChannel(Channel.fromChannel(guildChannel));
                    return guildChannel;
                })
                .subscribe();
        return server;
    }
}
