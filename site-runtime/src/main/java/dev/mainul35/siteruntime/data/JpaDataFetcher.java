package dev.mainul35.siteruntime.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * Fetches data directly from database using JPA/JDBC.
 * Only instantiated when site.runtime.database.type=jpa
 */
public class JpaDataFetcher implements DataFetcher {

    private static final Logger log = LoggerFactory.getLogger(JpaDataFetcher.class);

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Override
    public boolean supports(DataSourceType type) {
        return type == DataSourceType.DATABASE;
    }

    @Override
    public Object fetch(DataSourceConfig config, Map<String, String> params) {
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcTemplate not configured. Check database properties.");
        }

        String query = config.getQuery();
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Database query is required for DATABASE type");
        }

        log.debug("Executing database query: {}", query);

        List<Map<String, Object>> results = jdbcTemplate.queryForList(query);
        return results;
    }
}
