package org.eu.polarexpress.conductor.model;

import discord4j.core.object.entity.channel.GuildChannel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@ToString
public class Channel extends BaseEntity {
    @NotBlank
    @Setter
    private String snowflakeId;
    @NotBlank
    @Setter
    private String name;
    @Enumerated(EnumType.STRING)
    @Setter
    private discord4j.core.object.entity.channel.Channel.Type channelType;

    public static Channel fromChannel(GuildChannel guildChannel) {
        return Channel.builder()
                .snowflakeId(guildChannel.getId().asString())
                .name(guildChannel.getName())
                .channelType(guildChannel.getType())
                .build();
    }

    public static Channel merge(Channel channel, GuildChannel guildChannel) {
        channel.setSnowflakeId(guildChannel.getId().asString());
        channel.setName(guildChannel.getName());
        channel.setChannelType(guildChannel.getType());
        return channel;
    }
}
