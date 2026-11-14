package dev.mainul35.cms.sdk.runtime;

import java.util.Set;

/** Identity attached to a visitor session. Never client-writable. {@code avatarUrl} may be null. */
public record RuntimeUser(String id, String username, String displayName, String email, Set<String> roles,
                          String avatarUrl) {

    public RuntimeUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    public RuntimeUser(String id, String username, String displayName, String email, Set<String> roles) {
        this(id, username, displayName, email, roles, null);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
