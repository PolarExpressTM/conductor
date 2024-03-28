package org.eu.polarexpress.conductor.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.discord.DiscordBot;
import org.eu.polarexpress.conductor.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Controller
@RequestMapping("/audio")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class AudioController {
    private final UserService userService;
    private final DiscordBot discordBot;

    @GetMapping("/")
    public ModelAndView audioPage() {
        var view = new ModelAndView("audio");
        var currentTrack = discordBot.getAudioManager().getAudioPlayer().getPlayingTrack();
        view.addObject("currentTrack", currentTrack);
        view.addObject("queue", discordBot.getAudioManager().getQueue());
        view.addObject("loop", discordBot.getAudioManager().isLoop());
        view.addObject("paused", discordBot.getAudioManager().getAudioPlayer().isPaused());
        return view;
    }

    @GetMapping("/pause")
    public ModelAndView pauseTrack() {
        discordBot.getAudioManager().getAudioPlayer().setPaused(true);
        return new ModelAndView("redirect:/audio/");
    }

    @GetMapping("/continue")
    public ModelAndView continueTrack() {
        discordBot.getAudioManager().getAudioPlayer().setPaused(false);
        return new ModelAndView("redirect:/audio/");
    }

    @GetMapping("/skip")
    public ModelAndView skipCurrentTrack() {
        discordBot.getAudioManager().playNextTrack();
        return new ModelAndView("redirect:/audio/");
    }

    @GetMapping("/volume/{vol}")
    public ModelAndView changeVolume(@PathVariable int vol) {
        discordBot.getAudioManager().getAudioPlayer().setVolume(vol);
        return new ModelAndView("redirect:/audio/");
    }

    @GetMapping("/time/{timeframe}")
    public ModelAndView jumpToTimeframe(@PathVariable long timeframe) {
        Optional.ofNullable(discordBot.getAudioManager().getAudioPlayer().getPlayingTrack())
                .ifPresent(tf -> tf.setPosition(timeframe));
        return new ModelAndView("redirect:/audio/");
    }

    @GetMapping("/loop/{loop}")
    public ModelAndView jumpToTimeframe(@PathVariable boolean loop) {
        discordBot.getAudioManager().setLoop(loop);
        return new ModelAndView("redirect:/audio/");
    }

    @PostMapping("/add")
    public ModelAndView addTrack(@RequestParam String url) {
        discordBot.getAudioManager().addTrack(url);
        return new ModelAndView("redirect:/audio/");
    }

    @GetMapping("/remove/{index}")
    public ModelAndView removeTrack(@PathVariable int index) {
        discordBot.getAudioManager().removeTrack(index);
        return new ModelAndView("redirect:/audio/");
    }

    @GetMapping("/clear")
    public ModelAndView clearQueue() {
        discordBot.getAudioManager().clearQueue();
        return new ModelAndView("redirect:/audio/");
    }

}
