-- V10__add_auth_server_settings_table.sql
-- How the CMS signs people in through the VSD Auth Server, set by an administrator in the admin console.
-- One row at most (id 1). Without a row the settings come from the application configuration.
-- The client secret is stored encrypted.

CREATE TABLE IF NOT EXISTS auth_server_settings (
    id BIGINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    issuer_uri VARCHAR(500),
    authorization_uri VARCHAR(500),
    token_uri VARCHAR(500),
    jwk_set_uri VARCHAR(500),
    user_info_uri VARCHAR(500),
    client_id VARCHAR(200),
    client_secret_enc VARCHAR(1000),
    scopes VARCHAR(300),
    updated_by BIGINT,
    updated_at TIMESTAMP
);
