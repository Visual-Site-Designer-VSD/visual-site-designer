package dev.mainul35.vsd.runtime.data;

import com.fasterxml.jackson.databind.JsonNode;

/** Fetches the raw document of an API data source. Implementations must be safe to call from several threads. */
@FunctionalInterface
public interface SourceLoader {

    JsonNode load(SourceSpec spec) throws Exception;
}
