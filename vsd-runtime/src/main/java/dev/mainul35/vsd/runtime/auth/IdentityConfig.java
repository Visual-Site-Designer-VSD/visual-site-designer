package dev.mainul35.vsd.runtime.auth;

import dev.mainul35.cms.sdk.runtime.IdentityProvider;
import dev.mainul35.vsd.runtime.RuntimeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityConfig {

    /** Default provider; declare your own {@link IdentityProvider} bean to replace it. */
    @Bean
    @ConditionalOnMissingBean(IdentityProvider.class)
    IdentityProvider configIdentityProvider(RuntimeProperties props) {
        return new ConfigIdentityProvider(props);
    }
}
