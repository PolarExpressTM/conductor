package org.eu.polarexpress.conductor.discord.handler;

import com.deepl.api.DeepLException;
import com.deepl.api.Translator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class TranslationHandler {
    private final Logger logger = LoggerFactory.getLogger(PixivHandler.class);

    @Value("${discord.deepl}")
    private String deeplToken;
    @Getter
    private Translator translator;

    public void initTranslator() {
        try {
            translator = new Translator(deeplToken);
            var usage = translator.getUsage();
            if (usage.anyLimitReached()) {
                logger.error("DeepL monthly API used up!");
            }
        } catch (DeepLException | InterruptedException exception) {
            logger.error("initTranslator: {}", exception.getMessage());
        }
    }

    public String fetchUsageData() {
        try {
            var usage = translator.getUsage();
            if (usage.getCharacter() == null) {
                logger.error("Couldn't retrieve DeepL usage data!");
                return "";
            }
            var usageData = usage.getCharacter().getCount() + " / " + usage.getCharacter().getLimit();
            logger.info("fetchUsageData: {}", usageData);
            return usageData;
        } catch (DeepLException | InterruptedException exception) {
            logger.error("fetchUsageData: {}", exception.getMessage());
        }
        return "";
    }

    public String translate(String message, String targetLang) {
        try {
            var result = translator.translateText(message, null, targetLang);
            return result.getText();
        } catch (DeepLException | InterruptedException exception) {
            logger.error("translate: {}", exception.getMessage());
        }
        return "";
    }

}
