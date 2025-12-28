package dev.mainul35.siteruntime.data;

/**
 * Types of data sources supported by the system.
 */
public enum DataSourceType {
    API,       // External/internal REST API
    STATIC,    // Static data defined in config
    CONTEXT,   // Data from request context (user info, session, etc.)
    DATABASE   // Direct database query
}
