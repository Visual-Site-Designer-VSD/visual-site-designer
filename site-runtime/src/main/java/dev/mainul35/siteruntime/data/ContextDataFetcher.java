package dev.mainul35.siteruntime.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Fetches data from request context (user info, session data, etc.).
 */
public class ContextDataFetcher implements DataFetcher {

    private static final Logger log = LoggerFactory.getLogger(ContextDataFetcher.class);

    @Override
    public boolean supports(DataSourceType type) {
        return type == DataSourceType.CONTEXT;
    }

    @Override
    public Object fetch(DataSourceConfig config, Map<String, String> params) {
        String contextKey = config.getContextKey();

        if (contextKey == null || contextKey.isEmpty()) {
            log.warn("Context key is not specified");
            return null;
        }

        log.debug("Fetching context data for key: {}", contextKey);

        // Context data comes from request params
        if (params != null && params.containsKey(contextKey)) {
            return params.get(contextKey);
        }

        log.debug("No context data found for key: {}", contextKey);
        return null;
    }
}
