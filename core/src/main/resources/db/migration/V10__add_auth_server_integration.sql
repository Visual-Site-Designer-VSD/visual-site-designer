-- Add auth_server_id column for integration with VSD Auth Server
ALTER TABLE cms_users ADD COLUMN auth_server_id VARCHAR(36) UNIQUE;

-- Create index for faster lookups
CREATE INDEX idx_cms_users_auth_server_id ON cms_users(auth_server_id);
