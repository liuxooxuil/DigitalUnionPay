CREATE TABLE IF NOT EXISTS rbac_user (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  CONSTRAINT uk_rbac_user_username UNIQUE (username),
  CONSTRAINT uk_rbac_user_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS rbac_role (
  id BIGINT NOT NULL PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  name VARCHAR(128) NOT NULL,
  CONSTRAINT uk_rbac_role_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS rbac_permission (
  id BIGINT NOT NULL PRIMARY KEY,
  code VARCHAR(64) NOT NULL,
  http_method VARCHAR(16) NOT NULL,
  path_pattern VARCHAR(255) NOT NULL,
  CONSTRAINT uk_rbac_permission_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS rbac_user_role (
  user_id VARCHAR(64) NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  PRIMARY KEY (user_id, role_code)
);

CREATE TABLE IF NOT EXISTS rbac_role_permission (
  role_code VARCHAR(64) NOT NULL,
  permission_code VARCHAR(64) NOT NULL,
  PRIMARY KEY (role_code, permission_code)
);

CREATE TABLE IF NOT EXISTS auth_token (
  id BIGINT NOT NULL PRIMARY KEY,
  token_hash CHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  CONSTRAINT uk_auth_token_hash UNIQUE (token_hash)
);
