package dev.mainul35.plugins.ui;

import dev.mainul35.cms.sdk.runtime.RuntimeContext;
import dev.mainul35.cms.sdk.runtime.RuntimeEvent;
import dev.mainul35.cms.sdk.runtime.RuntimeEventHandler;
import dev.mainul35.cms.sdk.runtime.RuntimeResult;
import dev.mainul35.cms.sdk.runtime.StateKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Server-side half of the newsletter form in the VSD live runtime.
 *
 * <p>The form sends a {@code subscribe} event. The handler validates the address and records
 * the subscription in this plugin's session state ({@code PLUGIN/newsletter-form-plugin/subscription}),
 * which every open tab of the visitor sees at once. A production plugin would also hand the
 * address to its mailing-list service here; that part is deliberately not faked.
 */
public class NewsletterSubscribeHandler implements RuntimeEventHandler {

    public static final String PLUGIN_ID = "newsletter-form-plugin";
    public static final StateKey SUBSCRIPTION = StateKey.plugin(PLUGIN_ID, "subscription");

    private static final Logger log = LoggerFactory.getLogger(NewsletterSubscribeHandler.class);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Override
    public String pluginId() {
        return PLUGIN_ID;
    }

    @Override
    public Set<String> eventTypes() {
        return Set.of("subscribe");
    }

    @Override
    public RuntimeResult handle(RuntimeContext context, RuntimeEvent event) {
        String email = event.payload("email", String.class);
        email = email == null ? "" : email.trim();
        if (email.length() > 254 || !EMAIL.matcher(email).matches()) {
            return RuntimeResult.invalid("Please enter a valid email address.", Map.of("email", "invalid"));
        }

        Map<String, Object> subscription = new LinkedHashMap<>();
        subscription.put("email", email);
        subscription.put("subscribedAt", Instant.now().toString());
        subscription.put("signedInAs", context.user().map(u -> u.username()).orElse(null));
        context.state().set(SUBSCRIPTION, subscription);

        log.info("Newsletter subscription recorded for session on page '{}'", context.pageName());
        return RuntimeResult.ok("Subscribed");
    }
}
