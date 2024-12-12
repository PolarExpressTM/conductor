package org.eu.polarexpress.conductor.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateFields;
import org.htmlunit.BrowserVersion;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.htmlunit.options.HtmlUnitDriverOptions;
import org.openqa.selenium.htmlunit.options.HtmlUnitOption;
import reactor.core.publisher.Mono;
import ru.yandex.qatools.ashot.AShot;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

public class TierListCommand implements SlashCommand {
    @Override
    public String getName() {
        return "tierlist";
    }

    @Override
    public Mono<Message> handle(ChatInputInteractionEvent event) {
        return event.deferReply().then(getScreenshot(event));
    }

    private static Mono<Message> getScreenshot(ChatInputInteractionEvent event) {
        WebDriver webDriver = getWebDriver();
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        try {
            webDriver.get("https://www.prydwen.gg/wuthering-waves/tier-list");
        } catch (Exception ignored) {
        }
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        WebElement webElement = webDriver.findElement(By.cssSelector(".custom-tier-list-ww"));
        var screenshot = new AShot().takeScreenshot(webDriver, webElement);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            ImageIO.write(screenshot.getImage(), "jpeg", os);
            InputStream inputStream = new ByteArrayInputStream(os.toByteArray());
            return event.createFollowup().withFiles(MessageCreateFields.File.of(
                    "wuthering_waves_tier_list.jpg",
                    inputStream
            ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static WebDriver getWebDriver() {
        final HtmlUnitDriverOptions driverOptions = new HtmlUnitDriverOptions(BrowserVersion.FIREFOX, true);
        driverOptions.setCapability(HtmlUnitOption.optThrowExceptionOnScriptError, false);
        driverOptions.setCapability(HtmlUnitOption.optScreenWidth, 2560);
        driverOptions.setCapability(HtmlUnitOption.optScreenHeight, 1277);
        return new HtmlUnitDriver(driverOptions);
    }
}
