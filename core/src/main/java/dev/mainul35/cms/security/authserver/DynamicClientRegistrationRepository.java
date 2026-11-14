package dev.mainul35.cms.security.authserver;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The OAuth2 client registration for the VSD Auth Server, built from the settings in use at the moment of each
 * sign-in. Changing the settings in the admin console therefore applies at once, and nothing is contacted at startup
 * (the endpoints are used as configured, so the auth server needs no discovery document).
 *
 * <p>Deliberately not {@code Iterable}: Spring walks an Iterable repository while building the security filter chain,
 * which would try to read the registration before the auth server is reachable.
 */
public class DynamicClientRegistrationRepository implements ClientRegistrationRepository {

    public static final String REGISTRATION_ID = "vsd-auth";

    private final AuthServerSettingsService settings;
    private final String redirectUri;
    private final String userNameAttribute;

    private volatile long builtFor = -1;
    private volatile ClientRegistration cached;

    public DynamicClientRegistrationRepository(AuthServerSettingsService settings, String redirectUri, String userNameAttribute) {
        this.settings = settings;
        this.redirectUri = redirectUri;
        this.userNameAttribute = userNameAttribute;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!REGISTRATION_ID.equals(registrationId)) {
            return null;
        }
        AuthServerSettingsService.Effective e = settings.effective();
        if (!e.usable()) {
            return null; // sign-in through the auth server is off or not complete: Spring answers "unknown client"
        }
        long v = settings.version();
        ClientRegistration c = cached;
        if (c != null && builtFor == v && matches(c, e)) {
            return c;
        }
        c = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .authorizationUri(e.authorizationUri())
                .tokenUri(e.tokenUri())
                .jwkSetUri(e.jwkSetUri())
                .userInfoUri(blankToNull(e.userInfoUri()))
                // keeps the check that an ID token comes from this issuer
                .issuerUri(blankToNull(e.issuerUri()))
                .clientId(e.clientId())
                .clientSecret(e.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(scopes(e.scopes()))
                .clientName("VSD Auth Server")
                .userNameAttributeName(userNameAttribute)
                .build();
        cached = c;
        builtFor = v;
        return c;
    }

    /** Settings can also change in the database directly; rebuild if what is in use no longer matches. */
    private static boolean matches(ClientRegistration c, AuthServerSettingsService.Effective e) {
        return c.getClientId().equals(e.clientId())
                && c.getClientSecret().equals(e.clientSecret())
                && c.getProviderDetails().getAuthorizationUri().equals(e.authorizationUri())
                && c.getProviderDetails().getTokenUri().equals(e.tokenUri());
    }

    private static Set<String> scopes(String s) {
        return new LinkedHashSet<>(Arrays.asList(s.trim().split("[\\s,]+")));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
