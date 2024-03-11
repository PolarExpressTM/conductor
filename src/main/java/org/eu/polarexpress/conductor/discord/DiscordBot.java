package org.eu.polarexpress.conductor.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.discord.command.Command;
import org.eu.polarexpress.conductor.discord.detector.Detector;
import org.eu.polarexpress.conductor.discord.pixiv.PixivHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
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
    private final Map<String, Consumer<MessageCreateEvent>> detectors = new HashMap<>();
    @Getter
    private final AudioManager audioManager;
    @Getter
    private final PixivHandler pixivHandler;
    @Getter
    private GatewayDiscordClient client;
    @Getter
    private final Logger logger = LoggerFactory.getLogger(DiscordBot.class);

    @EventListener(ApplicationReadyEvent.class)
    public void startDiscordBot() {
        logger.info("Initiating Pixiv cookie...");
        pixivHandler.initCookie();
        logger.info("Initiating commands and detectors...");
        initCommands();
        initDetectors();
        logger.info("Connecting bot...");
        connect();
    }

    public void connect() {
        client = DiscordClientBuilder.create(token).build()
                .login()
                .block();
        if (client != null) {
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
            client.onDisconnect().block();
        }
    }

    private void initCommands() {
        var classes = getClasses("org.eu.polarexpress.conductor.discord.command");
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Command.class)) {
                    var commandName = method.getAnnotation(Command.class).command();
                    if (method.getReturnType() != Mono.class ||
                            method.getParameterCount() != 2 ||
                            method.getParameterTypes()[0] != DiscordBot.class ||
                            method.getParameterTypes()[1] != MessageCreateEvent.class) {
                        logger.error("Command \"{}\" has invalid return or parameter types!", commandName);
                        continue;
                    }
                    commands.put(commandName, event -> {
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
                            logger.error(exception.getMessage());
                        }
                        return Mono.empty();
                    });
                    logger.info("Registered command {}!", commandName);
                }
            }
        }
    }

    private void initDetectors() {
        var classes = getClasses("org.eu.polarexpress.conductor.discord.detector");
        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Detector.class)) {
                    var regex = method.getAnnotation(Detector.class).regex();
                    if (method.getParameterCount() != 2 ||
                            method.getParameterTypes()[0] != DiscordBot.class ||
                            method.getParameterTypes()[1] != MessageCreateEvent.class) {
                        logger.error("Detector with regex \"{}\" has invalid return or parameter types!", regex);
                        continue;
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
                    logger.info("Registered detector {}!", regex);
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
