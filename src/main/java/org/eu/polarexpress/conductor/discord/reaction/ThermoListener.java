package org.eu.polarexpress.conductor.discord.reaction;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.Color;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThermoListener {
    private static final String FOOTER_URL =
            "https://cdn.discordapp.com/avatars/996034025842036816/8f53fdf39c01cbb3474ed0eb0cd094a2.webp?size=100";

    @ReactionListener(emoji = "\uD83C\uDF21\uFE0F")
    public static Mono<Void> thermometer(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .mapNotNull(message -> thermometer(message.getContent()))
                .flatMap(result -> ev.getChannel().flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

    private static EmbedCreateSpec thermometer(String content) {
        int celsiusIdx = content.toUpperCase().indexOf("C");
        int fahrenheitIdx = content.toUpperCase().indexOf("F");
        if (celsiusIdx == -1 && fahrenheitIdx == -1) {
            return null;
        }
        float celsius = -1f;
        float fahrenheit = -1f;
        List<Integer> nums = findIntegers(content.substring(0, celsiusIdx != -1 ? celsiusIdx : fahrenheitIdx))
                .stream()
                .map(Integer::parseInt)
                .toList();
        if (!nums.isEmpty()) {
            if (celsiusIdx != -1) {
                celsius = nums.getLast();
                fahrenheit = (celsius * 1.8f) + 32;
            } else {
                fahrenheit = nums.getLast();
                celsius = (fahrenheit - 32) / 1.8f;
            }
        }
        return EmbedCreateSpec.builder()
                .color(Color.of(255, 67, 63))
                .addField("°C", String.valueOf(Math.round(celsius * 100.0) / 100.0), false)
                .addField("°F", String.valueOf(Math.round(fahrenheit * 100.0) / 100.0), false)
                .footer("Polar bear meat is delicious!", FOOTER_URL)
                .build();
    }

    private static List<String> findIntegers(String stringToSearch) {
        Pattern integerPattern = Pattern.compile("-?\\d+");
        Matcher matcher = integerPattern.matcher(stringToSearch);

        List<String> integerList = new ArrayList<>();
        while (matcher.find()) {
            integerList.add(matcher.group());
        }

        return integerList;
    }

}
