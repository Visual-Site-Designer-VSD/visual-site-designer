package dev.mainul35.cms.security.authserver;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Tells the sign-in page whether to offer the VSD Auth Server. Public, and says nothing but yes or no. */
@RestController
@RequestMapping("/api/auth")
public class AuthConfigController {

    private final AuthServerSettingsService settings;

    public AuthConfigController(AuthServerSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Map.of("ssoEnabled", settings.effective().usable(), "ssoName", "VSD Auth Server"));
    }
}
