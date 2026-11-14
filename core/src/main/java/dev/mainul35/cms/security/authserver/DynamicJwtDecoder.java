package dev.mainul35.cms.security.authserver;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Checks tokens issued by the VSD Auth Server (RS256, keys from its JWKS address) using the settings in use at the
 * moment. When sign-in through the auth server is off, such tokens are refused; tokens issued by the CMS itself are
 * handled elsewhere and never reach this decoder.
 */
public class DynamicJwtDecoder implements JwtDecoder {

    private final AuthServerSettingsService settings;

    private volatile String builtForUri;
    private volatile JwtDecoder delegate;

    public DynamicJwtDecoder(AuthServerSettingsService settings) {
        this.settings = settings;
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        AuthServerSettingsService.Effective e = settings.effective();
        if (!e.enabled() || e.jwkSetUri() == null || e.jwkSetUri().isBlank()) {
            throw new JwtException("Sign-in through the VSD Auth Server is turned off");
        }
        JwtDecoder d = delegate;
        if (d == null || !e.jwkSetUri().equals(builtForUri)) {
            synchronized (this) {
                if (delegate == null || !e.jwkSetUri().equals(builtForUri)) {
                    // Nothing is fetched until the first token arrives, so an unreachable auth server cannot stop startup.
                    delegate = NimbusJwtDecoder.withJwkSetUri(e.jwkSetUri()).build();
                    builtForUri = e.jwkSetUri();
                }
                d = delegate;
            }
        }
        return d.decode(token);
    }
}
