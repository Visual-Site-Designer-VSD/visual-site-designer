package dev.mainul35.siteruntime.config;

import dev.mainul35.siteruntime.controller.ApiProxyController;
import dev.mainul35.siteruntime.controller.PageApiController;
import dev.mainul35.siteruntime.controller.SpaController;
import dev.mainul35.siteruntime.data.*;
import dev.mainul35.siteruntime.service.PageDataService;
import dev.mainul35.siteruntime.service.InMemoryCacheProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import java.util.List;

/**
 * Auto-configuration for the Site Runtime.
 * Beans are created based on application.properties settings.
 */
@Configuration
@EnableConfigurationProperties(SiteRuntimeProperties.class)
public class SiteRuntimeAutoConfiguration {

    /**
     * REST API data fetcher - always available
     */
    @Bean
    @ConditionalOnMissingBean(RestApiDataFetcher.class)
    public RestApiDataFetcher restApiDataFetcher(SiteRuntimeProperties props) {
        return new RestApiDataFetcher(props.getApi());
    }

    /**
     * Static data fetcher - always available
     */
    @Bean
    @ConditionalOnMissingBean(StaticDataFetcher.class)
    public StaticDataFetcher staticDataFetcher() {
        return new StaticDataFetcher();
    }

    /**
     * Context data fetcher - always available
     */
    @Bean
    @ConditionalOnMissingBean(ContextDataFetcher.class)
    public ContextDataFetcher contextDataFetcher() {
        return new ContextDataFetcher();
    }

    /**
     * JPA data fetcher - only when JPA is enabled
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.database.type", havingValue = "jpa")
    public JpaDataFetcher jpaDataFetcher() {
        return new JpaDataFetcher();
    }

    /**
     * MongoDB data fetcher - only when MongoDB is enabled
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.database.type", havingValue = "mongodb")
    public MongoDataFetcher mongoDataFetcher() {
        return new MongoDataFetcher();
    }

    /**
     * In-memory cache - default fallback
     */
    @Bean
    @ConditionalOnProperty(name = "site.runtime.cache.type", havingValue = "memory", matchIfMissing = true)
    public InMemoryCacheProvider inMemoryCacheProvider(SiteRuntimeProperties props) {
        return new InMemoryCacheProvider(props.getCache().getDefaultTtlMs());
    }

    /**
     * Data source registry - collects all available fetchers
     */
    @Bean
    @ConditionalOnMissingBean(DataSourceRegistry.class)
    public DataSourceRegistry dataSourceRegistry(List<DataFetcher> fetchers) {
        return new DataSourceRegistry(fetchers);
    }

    /**
     * Page data service - handles page data fetching and caching
     */
    @Bean
    @ConditionalOnMissingBean(PageDataService.class)
    public PageDataService pageDataService(
            DataSourceRegistry dataSourceRegistry,
            ObjectMapper objectMapper,
            SiteRuntimeProperties props) {
        return new PageDataService(dataSourceRegistry, objectMapper, props);
    }

    /**
     * Object mapper - if not already configured
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * SPA fallback controller - forwards non-API routes to index.html
     */
    @Bean
    @ConditionalOnMissingBean(SpaController.class)
    public SpaController spaController() {
        return new SpaController();
    }

    /**
     * Page API controller - serves page data as JSON for the SPA
     */
    @Bean
    @ConditionalOnMissingBean(PageApiController.class)
    public PageApiController pageApiController(PageDataService pageDataService) {
        return new PageApiController(pageDataService);
    }

    /**
     * API Proxy controller - forwards authenticated requests from SPA to external APIs.
     * Only registered when auth is enabled and OAuth2AuthorizedClientService is available.
     */
    @Bean
    @ConditionalOnMissingBean(ApiProxyController.class)
    @ConditionalOnProperty(name = "site.runtime.auth.type", havingValue = "none", matchIfMissing = false)
    public ApiProxyController apiProxyControllerNoAuth(SiteRuntimeProperties props) {
        return new ApiProxyController(props, null);
    }

    @Bean
    @ConditionalOnMissingBean(ApiProxyController.class)
    public ApiProxyController apiProxyController(
            SiteRuntimeProperties props,
            OAuth2AuthorizedClientService authorizedClientService) {
        return new ApiProxyController(props, authorizedClientService);
    }
}
