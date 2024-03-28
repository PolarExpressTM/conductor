package org.eu.polarexpress.conductor.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.eu.polarexpress.conductor.discord.handler.TranslationHandler;
import org.eu.polarexpress.conductor.service.DiscordService;
import org.eu.polarexpress.conductor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import static org.eu.polarexpress.conductor.util.ContextUtils.getCurrentUser;

@Controller
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class HomeController {
    private final UserService userService;
    private final DiscordService discordService;
    private final TranslationHandler translationHandler;
    private final DiscordBot discordBot;

    @GetMapping
    public ModelAndView home() {
        var user = getCurrentUser(userService);
        if (user.isEmpty()) {
            return new ModelAndView("login");
        }
        var servers = discordService.findAllServers();
        var usageData = translationHandler.fetchUsageData();
        var track = discordBot.getAudioManager().getAudioPlayer().getPlayingTrack();
        discordBot.getLogger().info("Current track: {} {}", track.getInfo().title, track.getPosition());
        var view = new ModelAndView("home");
        view.addObject("user", user);
        view.addObject("servers", servers);
        view.addObject("usageData", usageData);
        return view;
    }

    @GetMapping("/login")
    public ModelAndView login() {
        return new ModelAndView("login");
    }

}
