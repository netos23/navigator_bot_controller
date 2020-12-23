package ru.fbtw.navigator.bot_controller.domain;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    SERVICE,
    USER;

    @Override
    public String getAuthority() {
        return name();
    }
}
