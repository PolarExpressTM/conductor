package org.eu.polarexpress.conductor.util;

import org.eu.polarexpress.conductor.model.Conductor;
import org.eu.polarexpress.conductor.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public final class ContextUtils {
    public static Optional<Conductor> getCurrentUser(UserService userService) {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(obj -> obj instanceof UserDetails)
                .map(obj -> (Conductor) userService.loadUserByUsername(((UserDetails) obj).getUsername()));
    }
}
