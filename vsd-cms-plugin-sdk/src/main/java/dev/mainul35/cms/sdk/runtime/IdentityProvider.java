package dev.mainul35.cms.sdk.runtime;

import java.util.Optional;

/**
 * SPI that turns credentials into an identity. The runtime owns the session; the provider
 * only answers the question "who is this?". Implement it for local users, the VSD auth
 * server, LDAP and so on.
 */
public interface IdentityProvider {

    /** @return the user if the credentials are valid, otherwise empty. */
    Optional<RuntimeUser> authenticate(String username, String password);
}
