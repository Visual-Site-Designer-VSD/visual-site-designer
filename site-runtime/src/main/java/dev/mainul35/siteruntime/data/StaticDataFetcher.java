package dev.mainul35.siteruntime.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Fetches static data from configuration.
 */
public class StaticDataFetcher implements DataFetcher {

    private static final Logger log = LoggerFactory.getLogger(StaticDataFetcher.class);

    @Override
    public boolean supports(DataSourceType type) {
        return type == DataSourceType.STATIC;
    }

    @Override
    public Object fetch(DataSourceConfig config, Map<String, String> params) {
        log.debug("Returning static data");
        return config.getStaticData();
    }
}
