package org.eu.polarexpress.conductor.discord.reaction;

import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.ReactionAddEvent;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class TranslationListener {

    @ReactionListener(emoji = "\uD83C\uDDE8\uD83C\uDDF3")
    public static Mono<Void> translateCN(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .map(message -> bot.getTranslationHandler().translate(message.getContent(), "ZH"))
                .flatMap(result -> ev.getChannel().flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

    @ReactionListener(emoji = "\uD83C\uDDE9\uD83C\uDDEA")
    public static Mono<Void> translateDE(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .map(message -> bot.getTranslationHandler().translate(message.getContent(), "DE"))
                .flatMap(result -> ev.getChannel().flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

    @ReactionListener(emoji = "\uD83C\uDDFA\uD83C\uDDF8")
    public static Mono<Void> translateEN(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .map(message -> bot.getTranslationHandler().translate(message.getContent(), "EN"))
                .flatMap(result -> ev.getChannel().flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

    @ReactionListener(emoji = "\uD83C\uDDEB\uD83C\uDDF7")
    public static Mono<Void> translateFR(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .map(message -> bot.getTranslationHandler().translate(message.getContent(), "FR"))
                .flatMap(result -> ev.getChannel().flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

    @ReactionListener(emoji = "\uD83C\uDDEF\uD83C\uDDF5")
    public static Mono<Void> translateJP(DiscordBot bot, Event event) {
        var ev = (ReactionAddEvent) event;
        return ev.getMessage()
                .publishOn(Schedulers.boundedElastic())
                .map(message -> bot.getTranslationHandler().translate(message.getContent(), "JA"))
                .flatMap(result -> ev.getChannel().flatMap(channel -> channel.createMessage(result)).then())
                .then();
    }

}
