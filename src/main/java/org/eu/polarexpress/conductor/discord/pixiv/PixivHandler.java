package org.eu.polarexpress.conductor.discord.pixiv;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PixivHandler {

    private static final String BASE_URL = "https://pixiv.net";
    private static final String API_URL = "https://pixiv.net";

    @Value("${pixiv.session}")
    private String sessionCookie;

    public boolean login() {
        // TODO send req to base url and check for logout btn + etc
        return true;
    }

}
