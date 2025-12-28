package dev.mainul35.siteruntime.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.Map;

/**
 * Fetches data from MongoDB.
 * Only instantiated when site.runtime.database.type=mongodb
 */
public class MongoDataFetcher implements DataFetcher {

    private static final Logger log = LoggerFactory.getLogger(MongoDataFetcher.class);

    @Autowired(required = false)
    private MongoTemplate mongoTemplate;

    @Override
    public boolean supports(DataSourceType type) {
        return type == DataSourceType.DATABASE;
    }

    @Override
    public Object fetch(DataSourceConfig config, Map<String, String> params) {
        if (mongoTemplate == null) {
            throw new IllegalStateException("MongoTemplate not configured. Check MongoDB properties.");
        }

        String collectionName = config.getQuery(); // Repurpose query field for collection name
        if (collectionName == null || collectionName.isEmpty()) {
            throw new IllegalArgumentException("Collection name is required for MongoDB type");
        }

        log.debug("Fetching from MongoDB collection: {}", collectionName);

        // Simple find all - could be enhanced with query parameters
        List<?> results = mongoTemplate.findAll(Map.class, collectionName);
        return results;
    }
}
