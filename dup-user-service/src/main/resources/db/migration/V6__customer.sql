ALTER TABLE rbac_user ADD COLUMN phone_e164 VARCHAR(24) NULL;
ALTER TABLE rbac_user ADD COLUMN email VARCHAR(254) NULL;
ALTER TABLE rbac_user ADD COLUMN failed_logins INT NOT NULL DEFAULT 0;
ALTER TABLE rbac_user ADD COLUMN locked_until DATETIME NULL;
ALTER TABLE rbac_user ADD COLUMN mfa_enabled TINYINT NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX uk_rbac_user_phone ON rbac_user (phone_e164);
CREATE UNIQUE INDEX uk_rbac_user_email ON rbac_user (email);

ALTER TABLE auth_token ADD COLUMN device_id VARCHAR(64) NULL;
ALTER TABLE auth_token ADD COLUMN device_name VARCHAR(128) NULL;
ALTER TABLE auth_token ADD COLUMN client_ip VARCHAR(64) NULL;
ALTER TABLE auth_token ADD COLUMN location_key VARCHAR(64) NULL;
ALTER TABLE auth_token ADD COLUMN last_seen_at DATETIME NULL;
ALTER TABLE auth_token ADD COLUMN revoked_at DATETIME NULL;

ALTER TABLE user_profile ADD COLUMN kyc_level VARCHAR(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE fund ADD COLUMN fund_kind VARCHAR(16) NOT NULL DEFAULT 'FIAT';

CREATE TABLE IF NOT EXISTS verification_code (
  id BIGINT NOT NULL PRIMARY KEY,
  channel VARCHAR(16) NOT NULL,
  target VARCHAR(254) NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  code_hash CHAR(64) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  expires_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL,
  consumed_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS registration_event (
  id BIGINT NOT NULL PRIMARY KEY,
  client_ip VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS device_credential (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  device_id VARCHAR(64) NOT NULL,
  public_key VARCHAR(1024) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_device_credential UNIQUE (user_id, device_id)
);

CREATE TABLE IF NOT EXISTS biometric_challenge (
  id BIGINT NOT NULL PRIMARY KEY,
  device_id VARCHAR(64) NOT NULL,
  challenge CHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  CONSTRAINT uk_biometric_challenge UNIQUE (challenge)
);

CREATE TABLE IF NOT EXISTS kyc_case (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  full_name VARCHAR(128) NOT NULL,
  birth_date DATE NOT NULL,
  nationality CHAR(2) NOT NULL,
  address VARCHAR(500) NOT NULL,
  id_type VARCHAR(32) NOT NULL,
  id_number_cipher VARCHAR(512) NOT NULL,
  front_sha256 CHAR(64) NOT NULL,
  back_sha256 CHAR(64) NOT NULL,
  liveness_score INT NULL,
  face_score INT NULL,
  sanctions_result VARCHAR(32) NOT NULL,
  rfi_due_at DATETIME NULL,
  reject_reason VARCHAR(255) NULL,
  submitted_at DATETIME NOT NULL,
  decided_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS kyc_document (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  side VARCHAR(16) NOT NULL,
  sha256 CHAR(64) NOT NULL,
  content_type VARCHAR(64) NOT NULL,
  stored_path VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS liveness_session (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  session_id VARCHAR(64) NOT NULL,
  expires_at DATETIME NOT NULL,
  consumed_at DATETIME NULL,
  CONSTRAINT uk_liveness_session UNIQUE (session_id)
);

CREATE TABLE IF NOT EXISTS sanctions_entry (
  id BIGINT NOT NULL PRIMARY KEY,
  name_key VARCHAR(256) NOT NULL,
  source VARCHAR(32) NOT NULL,
  CONSTRAINT uk_sanctions_name UNIQUE (name_key, source)
);

CREATE TABLE IF NOT EXISTS airwallex_account (
  user_id VARCHAR(64) NOT NULL PRIMARY KEY,
  external_id VARCHAR(64) NULL,
  status VARCHAR(32) NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS bank_account (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  holder_name VARCHAR(128) NOT NULL,
  account_cipher VARCHAR(512) NOT NULL,
  bank_name VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS kyc_limit (
  kyc_level VARCHAR(32) NOT NULL PRIMARY KEY,
  single_minor BIGINT NOT NULL,
  daily_minor BIGINT NOT NULL,
  monthly_minor BIGINT NOT NULL,
  currency VARCHAR(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS closure_request (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  status VARCHAR(32) NOT NULL,
  requested_at DATETIME NOT NULL,
  decided_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS inheritance_case (
  id BIGINT NOT NULL PRIMARY KEY,
  case_id VARCHAR(64) NOT NULL,
  estate_user_id VARCHAR(64) NOT NULL,
  heir_user_id VARCHAR(64) NOT NULL,
  applicant_role VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  evidence_sha256 CHAR(64) NOT NULL,
  first_reviewer VARCHAR(64) NULL,
  second_reviewer VARCHAR(64) NULL,
  opened_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uk_inheritance_case UNIQUE (case_id)
);

INSERT INTO fund (id, fund_code, name, currency, scale, status, fund_kind)
SELECT 1, 'MAIN', '主账户', 'CNY', 2, 'OPEN', 'FIAT' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM fund WHERE fund_code = 'MAIN');

INSERT INTO kyc_limit (kyc_level, single_minor, daily_minor, monthly_minor, currency)
SELECT 'NONE', 0, 0, 0, 'CNY' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM kyc_limit WHERE kyc_level = 'NONE');

INSERT INTO kyc_limit (kyc_level, single_minor, daily_minor, monthly_minor, currency)
SELECT 'BASIC', 5000000, 20000000, 100000000, 'CNY' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM kyc_limit WHERE kyc_level = 'BASIC');

INSERT INTO rbac_role (id, code, name)
SELECT 100, 'CUSTOMER', '客户' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_role WHERE code = 'CUSTOMER');

INSERT INTO rbac_permission (id, code, http_method, path_pattern)
SELECT 100, 'CUSTOMER_USERS', '*', '/internal/users/**' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_permission WHERE code = 'CUSTOMER_USERS');

INSERT INTO rbac_permission (id, code, http_method, path_pattern)
SELECT 101, 'CUSTOMER_SESSIONS', '*', '/internal/sessions/**' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_permission WHERE code = 'CUSTOMER_SESSIONS');

INSERT INTO rbac_permission (id, code, http_method, path_pattern)
SELECT 102, 'CUSTOMER_SECURITY', '*', '/internal/auth/security/**' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_permission WHERE code = 'CUSTOMER_SECURITY');

INSERT INTO rbac_role_permission (role_code, permission_code)
SELECT 'CUSTOMER', 'CUSTOMER_USERS' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_role_permission WHERE role_code = 'CUSTOMER' AND permission_code = 'CUSTOMER_USERS');

INSERT INTO rbac_role_permission (role_code, permission_code)
SELECT 'CUSTOMER', 'CUSTOMER_SESSIONS' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_role_permission WHERE role_code = 'CUSTOMER' AND permission_code = 'CUSTOMER_SESSIONS');

INSERT INTO rbac_role_permission (role_code, permission_code)
SELECT 'CUSTOMER', 'CUSTOMER_SECURITY' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM rbac_role_permission WHERE role_code = 'CUSTOMER' AND permission_code = 'CUSTOMER_SECURITY');
