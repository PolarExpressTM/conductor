package org.eu.polarexpress.conductor.discord.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.eu.polarexpress.conductor.dto.UrbanResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class UrbanCommand {
    private static final String API_URL = "https://unofficialurbandictionaryapi.com/api/search" +
            "?term=%s&strict=false&matchCase=false&limit=none&page=1&multiPage=false";

    @Command(command = "df")
    public static Mono<Void> findDefinition(DiscordBot bot, MessageCreateEvent event) {
        return Mono.justOrEmpty(event.getMessage())
                .publishOn(Schedulers.boundedElastic())
                .map(message -> findUrban(bot, message))
                .flatMap(result -> event.getMessage().getChannel()
                        .flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

    private static EmbedCreateSpec findUrban(DiscordBot bot, Message message) {
        var args = Arrays.asList(message.getContent().split(" "));
        if (args.size() < 2) {
            return null;
        }
        try {
            var response = bot.getHttpHandler().getJson(String.format(API_URL, args.get(1)),
                            "Referer", "https://unofficialurbandictionaryapi.com/")
                    .thenApply(HttpResponse::body)
                    .get();
            var objectMapper = bot.getHttpHandler().getObjectMapper();
            var urbanResponse = objectMapper.readValue(response, UrbanResponse.class);
            bot.getLogger().info("findDefinition: {}", objectMapper.writeValueAsString(urbanResponse));
            var fields = new ArrayList<EmbedCreateFields.Field>();
            for (var data : urbanResponse.data().stream().limit(3).toList()) {
                fields.add(EmbedCreateFields.Field.of(
                        data.word(),
                        data.meaning(),
                        false
                ));
            }
            return EmbedCreateSpec.builder()
                    .color(Color.of(255, 67, 63))
                    .title("Urban Dictionary")
                    .addFields(fields.toArray(new EmbedCreateFields.Field[0]))
                    .build();
        } catch (InterruptedException | ExecutionException | JsonProcessingException e) {
            return null;
        }
    }

}
