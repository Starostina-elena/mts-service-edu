package ru.aigul.mts_service.auth;

import java.security.Principal;

public class RolePrincipal implements Principal {
    private final String name;

    public RolePrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "RolePrincipal{" + name + '}';
    }
}

