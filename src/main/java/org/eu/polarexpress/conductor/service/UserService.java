package org.eu.polarexpress.conductor.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.eu.polarexpress.conductor.model.Conductor;
import org.eu.polarexpress.conductor.repository.ConductorRepository;
import org.eu.polarexpress.conductor.util.ObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @Autowired)
public class UserService implements UserDetailsService {
    private final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final PasswordEncoder passwordEncoder;
    private final ConductorRepository conductorRepository;
    @Value("${discord.user-agent}")
    private String userAgent;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return loadUser(username, false);
    }

    public OAuth2User loadOAuth2User(OAuth2UserRequest userRequest) {
        return processOAuthRequest(userRequest, userAttributes -> {
            String snowflake = (String) userAttributes.get("id");
            String username = (String) userAttributes.get("username");
            String globalName = (String) userAttributes.get("global_name");
            String locale = (String) userAttributes.get("locale");
            var temp = conductorRepository.findBySnowflakeId(snowflake);
            if (temp.isPresent()) {
                temp.get().setUsername(username);
                temp.get().setGlobalName(globalName);
                temp.get().setLocale(locale);
                return loadUser(snowflake, true);
            }
            Conductor conductor = Conductor.builder()
                    .snowflakeId(snowflake)
                    .username(username)
                    .globalName(globalName)
                    .locale(locale)
                    .build();
            conductorRepository.save(conductor);
            return loadUser(snowflake, true);
        });
    }

    public OidcUser loadOicdUser(OidcUserRequest userRequest) {
        return processOAuthRequest(userRequest, userAttributes -> {
            logger.warn("loadOicdUser should not have been triggered!");
            return null;
        });
    }

    private Conductor loadUser(String username, boolean oauth2) {
        if (!oauth2 && (username == null || username.contains("@oauth2.oauth2"))) {
            throw new UsernameNotFoundException("User with the email " + username + " not found!");
        }
        Optional<Conductor> user;
        if (oauth2) {
            user = conductorRepository.findBySnowflakeId(username);
        } else {
            user = conductorRepository.findByUsername(username);
        }
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new UsernameNotFoundException("User with the email " + username + " not found!");
        }
    }

    private Conductor processOAuthRequest(OAuth2UserRequest userRequest, Function<ObjectMap, Conductor> callback) {
        String uri = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUri();
        if (uri != null && !uri.isEmpty()) {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.AUTHORIZATION,
                    "Bearer " + userRequest.getAccessToken().getTokenValue());
            headers.set(HttpHeaders.USER_AGENT, userAgent);
            HttpEntity<Object> entity = new HttpEntity<>("", headers);
            ResponseEntity<ObjectMap> response = restTemplate
                    .exchange(uri, HttpMethod.GET, entity, ObjectMap.class);
            ObjectMap userAttributes = response.getBody();
            if (userAttributes != null) {
                // userAttributes.forEach((key, value) -> logger.info("{}\t{}", key, value));
                return callback.apply(userAttributes);
            }
        }
        throw new UsernameNotFoundException("OAuth2 failed!");
    }
}
