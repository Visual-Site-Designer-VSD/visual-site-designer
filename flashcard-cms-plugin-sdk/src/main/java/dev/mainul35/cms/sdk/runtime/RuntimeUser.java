package dev.mainul35.cms.sdk.runtime;

import java.util.Set;

/** Identity attached to a visitor session. Never client-writable. */
public record RuntimeUser(String id, String username, String displayName, String email, Set<String> roles) {

    public RuntimeUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
