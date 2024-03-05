package org.eu.polarexpress.conductor.discord.pixiv;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PixivHandler {

    private static final String BASE_URL = "https://www.pixiv.net";
    private static final String API_URL = "https://www.pixiv.net/ajax";

    @Value("${pixiv.session}")
    private String sessionCookie;

    public boolean login() {
        // TODO send req to base url and check for logout btn + etc
        /*
        Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36
        Name = "PHPSESSID"
        Domain = "pixiv.net"
        Path = "/"
        HttpOnly = false
        Secure = false
         */
        var response = "";
        return response.contains("logout.php") ||
                response.contains("pixiv.user.loggedIn = true") ||
                response.contains("_gaq.push(['_setCustomVar', 1, 'login', 'yes'") ||
                response.contains("var dataLayer = [{ login: 'yes',");
    }

    public String getIllustration(String id) {
        return API_URL + "/illust/" + id;
    }

    public String getIllustrationPages(String id) {
        return API_URL + "/illust/" + id + "/pages";
    }

    public String getUgoiraMetadata(String id) {
        return API_URL + "/illust/" + id + "/ugoira_meta";
    }

}
