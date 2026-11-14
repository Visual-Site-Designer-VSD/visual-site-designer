package dev.mainul35.cms.security.authserver;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * How this CMS signs people in through the VSD Auth Server, as set by an administrator in the admin console.
 * There is at most one row (id 1). Without a row the settings come from the application configuration.
 * The client secret is stored encrypted and is never sent back to the browser.
 */
@Entity
@Table(name = "auth_server_settings")
@Getter
@Setter
@NoArgsConstructor
public class AuthServerSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(nullable = false)
    private boolean enabled;

    @Column(length = 500)
    private String issuerUri;

    @Column(length = 500)
    private String authorizationUri;

    @Column(length = 500)
    private String tokenUri;

    @Column(length = 500)
    private String jwkSetUri;

    @Column(length = 500)
    private String userInfoUri;

    @Column(length = 200)
    private String clientId;

    /** AES-GCM, base64: never the plain secret. */
    @Column(length = 1000)
    private String clientSecretEnc;

    @Column(length = 300)
    private String scopes = "openid profile email";

    private Long updatedBy;

    private Instant updatedAt;
}
