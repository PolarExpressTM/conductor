package org.eu.polarexpress.conductor.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.discord.command.Command;
import org.eu.polarexpress.conductor.discord.command.SlashCommandListener;
import org.eu.polarexpress.conductor.discord.detector.Detector;
import org.eu.polarexpress.conductor.discord.event.Listener;
import org.eu.polarexpress.conductor.discord.reaction.ReactionListener;
import org.eu.polarexpress.conductor.discord.handler.PixivHandler;
import org.eu.polarexpress.conductor.discord.handler.TranslationHandler;
import org.eu.polarexpress.conductor.service.DiscordService;
import org.eu.polarexpress.conductor.util.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class DiscordBot {
    @Value("${discord.prefix}")
    private String prefix;
    @Value("${discord.token}")
    private String token;
    private final Map<String, Function<MessageCreateEvent, Mono<Void>>> commands = new HashMap<>();
    private final Map<String, Function<Event, Mono<Void>>> listeners = new HashMap<>();
    private final Map<String, Consumer<MessageCreateEvent>> detectors = new HashMap<>();
    @Getter
    private final HttpHandler httpHandler;
    @Getter
    private final AudioManager audioManager;
    @Getter
    private final PixivHandler pixivHandler;
    @Getter
    private final TranslationHandler translationHandler;
    @Getter
    private final DiscordService discordService;
    @Getter
    private GatewayDiscordClient client;
    @Getter
    private final Logger logger = LoggerFactory.getLogger(DiscordBot.class);

    @EventListener(ApplicationReadyEvent.class)
    public void startDiscordBot() {
        logger.info("Initiating handlers...");
        pixivHandler.initCookie();
        translationHandler.initTranslator();
        logger.info("Initiating commands...");
        initCommands();
        logger.info("Initiating listeners...");
        initListeners();
        logger.info("Initiating detectors...");
        initDetectors();
        logger.info("Connecting bot...");
        connect();
    }

    public void connect() {
        client = DiscordClientBuilder.create(token).build()
                .login()
                .block();
        if (client != null) {
            client.getGuilds()
                    .publishOn(Schedulers.boundedElastic())
                    .map(guild -> {
                        logger.info("Guild: {}\t{}\t{}",
                                guild.getId().asString(), guild.getName(), guild.getMemberCount());
                        discordService.save(guild);
                        return guild;
                    })
                    .subscribe();
            client.getEventDispatcher().on(GuildCreateEvent.class)
                    .flatMap(event -> Mono.just(event)
                            .filter(ev -> listeners.containsKey(ev.getClass().getName()))
                            .flatMap(ev -> listeners.get(ev.getClass().getName()).apply(ev)))
                    .subscribe();
            client.getEventDispatcher().on(MessageCreateEvent.class)
                    .flatMap(event -> Mono.just(event.getMessage().getContent())
                            .map(string -> {
                                detectors.entrySet().stream()
                                        .filter(entry -> string.matches(entry.getKey()))
                                        .forEach(entry -> entry.getValue().accept(event));
                                return string;
                            })
                            .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                    .filter(entry -> content.startsWith(prefix + entry.getKey()))
                                    .flatMap(entry -> entry.getValue().apply(event))
                                    .next()))
                    .subscribe();
            client.getEventDispatcher().on(ReactionAddEvent.class)
                    .flatMap(event -> Mono.just(event)
                            .filter(ev -> listeners.containsKey(ev.getEmoji().asUnicodeEmoji()
                                    .map(ReactionEmoji.Unicode::getRaw)
                                    .orElse(null)))
                            .flatMap(ev -> listeners.get(ev.getEmoji().asUnicodeEmoji()
                                            .map(ReactionEmoji.Unicode::getRaw)
                                            .orElse(null))
                                    .apply(ev)))
                    .subscribe();
            try {
                new GlobalCommandRegistrar(client.getRestClient()).registerCommands(List.of(
                        "tierlist.json"
                ));
            } catch (Exception e) {
                logger.error("Failed to load slash commands", e);
            }
            client.on(ChatInputInteractionEvent.class, SlashCommandListener::handle).subscribe();
            client.onDisconnect().block();
        }
    }

    private void initCommands() {
        scanAnnotations("org.eu.polarexpress.conductor.discord.command", Command.class, method -> {
            var commandName = method.getAnnotation(Command.class).command();
            if (method.getReturnType() != Mono.class ||
                    method.getParameterCount() != 2 ||
                    method.getParameterTypes()[0] != DiscordBot.class ||
                    method.getParameterTypes()[1] != MessageCreateEvent.class) {
                logger.error("Command \"{}\" has invalid return or parameter types!", commandName);
                return;
            }
            commands.put(commandName, event -> invokeMethod(method, event));
            logger.info("Registered command \"{}\"!", commandName);
        });
    }

    private void initListeners() {
        scanAnnotations("org.eu.polarexpress.conductor.discord.event", Listener.class, method -> {
            var eventClass = method.getAnnotation(Listener.class).event();
            if (method.getReturnType() != Mono.class ||
                    method.getParameterCount() != 2 ||
                    method.getParameterTypes()[0] != DiscordBot.class ||
                    method.getParameterTypes()[1] != Event.class) {
                logger.error("Listener for \"{}\" has invalid return or parameter types!", eventClass.getName());
                return;
            }
            listeners.put(eventClass.getName(), event -> invokeMethod(method, event));
            logger.info("Registered listener for \"{}\"!", eventClass.getName());
        });
        scanAnnotations("org.eu.polarexpress.conductor.discord.reaction", ReactionListener.class, method -> {
            var emoji = method.getAnnotation(ReactionListener.class).emoji();
            if (method.getReturnType() != Mono.class ||
                    method.getParameterCount() != 2 ||
                    method.getParameterTypes()[0] != DiscordBot.class ||
                    method.getParameterTypes()[1] != Event.class) {
                logger.error("ReactionListener for emoji \"{}\" has invalid return or parameter types!",
                        emoji);
                return;
            }
            listeners.put(emoji, event -> invokeMethod(method, event));
            logger.info("Registered listener for emoji \"{}\"!", emoji);
        });
    }

    private void initDetectors() {
        scanAnnotations("org.eu.polarexpress.conductor.discord.detector", Detector.class, method -> {
            var regex = method.getAnnotation(Detector.class).regex();
            if (method.getParameterCount() != 2 ||
                    method.getParameterTypes()[0] != DiscordBot.class ||
                    method.getParameterTypes()[1] != MessageCreateEvent.class) {
                logger.error("Detector with regex \"{}\" has invalid return or parameter types!", regex);
                return;
            }
            detectors.put(regex, event -> {
                try {
                    var access = method.canAccess(null);
                    if (!access) {
                        method.setAccessible(true);
                    }
                    method.invoke(null, this, event);
                    if (!access) {
                        method.setAccessible(false);
                    }
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    logger.error(exception.getMessage());
                }
            });
            logger.info("Registered detector \"{}\"!", regex);
        });
    }

    private Mono<Void> invokeMethod(Method method, Event event) {
        try {
            var access = method.canAccess(null);
            if (!access) {
                method.setAccessible(true);
            }
            var result = method.invoke(null, this, event);
            if (!access) {
                method.setAccessible(false);
            }
            return result instanceof Mono ? (Mono<Void>) result : Mono.empty();
        } catch (IllegalAccessException | InvocationTargetException exception) {
            logger.error("{}: {}", method.getName(), exception.getMessage());
        }
        return Mono.empty();
    }

    private void scanAnnotations(String packageName,
                                 Class<? extends Annotation> annotation,
                                 Consumer<Method> callback) {
        var classes = getClasses(packageName);
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    callback.accept(method);
                }
            }
        }
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package and subpackages.
     *
     * @param packageName
     *            The base package
     * @return The classes
     */
    private List<Class<?>> getClasses(String packageName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            Enumeration<URL> resources = classLoader.getResources(path);
            List<File> dirs = new ArrayList<>();
            while (resources.hasMoreElements())
            {
                URL resource = resources.nextElement();
                URI uri = new URI(resource.toString());
                dirs.add(new File(uri.getPath()));
            }
            List<Class<?>> classes = new ArrayList<>();
            for (File directory : dirs)
            {
                classes.addAll(findClasses(directory, packageName));
            }
            return classes;
        } catch (ClassNotFoundException | IOException | URISyntaxException exception) {
            logger.error(exception.getMessage());
        }
        return List.of();
    }

    /**
     * Recursive method used to find all classes in a given directory and
     * sub dirs.
     *
     * @param directory
     *            The base directory
     * @param packageName
     *            The package name for classes found inside the base directory
     * @return The classes
     */
    private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException
    {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists())
        {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return List.of();
        }
        for (File file : files)
        {
            if (file.isDirectory())
            {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            }
            else if (file.getName().endsWith(".class"))
            {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }
}
