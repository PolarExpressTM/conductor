package org.eu.polarexpress.conductor.discord;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import lombok.Getter;
import org.eu.polarexpress.conductor.discord.command.Command;
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
import java.util.function.Function;

@Component
public class DiscordBot {
    @Value("${discord.prefix}")
    private String prefix;
    @Value("${discord.token}")
    private String token;
    private final Map<String, Function<MessageCreateEvent, Mono<Void>>> commands;
    @Getter
    private final AudioManager audioManager;
    @Getter
    private GatewayDiscordClient client;
    @Getter
    private final Logger logger = LoggerFactory.getLogger(DiscordBot.class);

    @Autowired
    public DiscordBot(AudioManager audioManager) {
        commands = new HashMap<>();
        this.audioManager = audioManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startDiscordBot() {
        logger.info("Connecting bot...");
        initCommands();
        connect();
    }

    public void connect() {
        client = DiscordClientBuilder.create(token).build()
                .login()
                .block();
        if (client != null) {
            client.getEventDispatcher().on(MessageCreateEvent.class)
                    .flatMap(event -> Mono.just(event.getMessage().getContent())
                            .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                    .filter(entry -> content.startsWith(prefix + entry.getKey()))
                                    .flatMap(entry -> entry.getValue().apply(event))
                                    .next()))
                    .subscribe();
            client.onDisconnect().block();
        }
    }

    public Map<String, Function<MessageCreateEvent, Mono<Void>>> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    private void initCommands() {
        var classes = getClasses("org.eu.polarexpress.conductor.discord.command");

        for (Class<?> clazz : classes) {
            for (Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(Command.class)) {
                    var commandName = method.getAnnotation(Command.class).command();
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
            List<File> dirs = new ArrayList<File>();
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
