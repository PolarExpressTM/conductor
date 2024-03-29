package org.eu.polarexpress.conductor.discord.event;

import discord4j.core.event.domain.Event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Listener {
    Class<? extends Event> event();
}
