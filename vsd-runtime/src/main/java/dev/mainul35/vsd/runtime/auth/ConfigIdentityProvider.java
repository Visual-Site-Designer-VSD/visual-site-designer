package dev.mainul35.vsd.runtime.auth;

import dev.mainul35.cms.sdk.runtime.IdentityProvider;
import dev.mainul35.cms.sdk.runtime.RuntimeUser;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Optional;

/**
 * Identity provider backed by {@code vsd.runtime.users}. Passwords are compared in
 * plain text, so this is for development and tiny sites only. Define your own
 * {@link IdentityProvider} bean (for example one that calls the VSD auth server) to replace it.
 */
public class ConfigIdentityProvider implements IdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(ConfigIdentityProvider.class);

    private final RuntimeProperties props;

    public ConfigIdentityProvider(RuntimeProperties props) {
        this.props = props;
        if (props.getUsers().isEmpty()) {
            log.warn("No vsd.runtime.users configured: nobody can sign in");
        } else {
            log.warn("Using the config identity provider (plain-text passwords): development use only");
        }
    }

    @Override
    public Optional<RuntimeUser> authenticate(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }
        byte[] given = password.getBytes(StandardCharsets.UTF_8);
        RuntimeUser match = null;
        // Check every user so timing does not reveal which usernames exist.
        for (RuntimeProperties.User u : props.getUsers()) {
            boolean nameOk = username.equalsIgnoreCase(u.getUsername()) || username.equalsIgnoreCase(u.getEmail());
            boolean passOk = u.getPassword() != null
                    && MessageDigest.isEqual(given, u.getPassword().getBytes(StandardCharsets.UTF_8));
            if (nameOk && passOk && match == null) {
                match = new RuntimeUser(
                        u.getId() != null ? u.getId() : u.getUsername(),
                        u.getUsername(),
                        u.getDisplayName() != null ? u.getDisplayName() : u.getUsername(),
                        u.getEmail(),
                        new HashSet<>(u.getRoles()));
            }
        }
        return Optional.ofNullable(match);
    }
}
