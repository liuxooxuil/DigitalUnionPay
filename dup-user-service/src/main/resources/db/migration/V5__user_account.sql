CREATE TABLE IF NOT EXISTS user_profile (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  user_status VARCHAR(32) NOT NULL,
  kyc_status VARCHAR(32) NOT NULL,
  account_status VARCHAR(32) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_user_profile_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS fund (
  id BIGINT NOT NULL PRIMARY KEY,
  fund_code VARCHAR(16) NOT NULL,
  name VARCHAR(128) NOT NULL,
  currency VARCHAR(16) NOT NULL,
  scale INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  CONSTRAINT uk_fund_code UNIQUE (fund_code)
);

CREATE TABLE IF NOT EXISTS fund_account (
  id BIGINT NOT NULL PRIMARY KEY,
  account_no VARCHAR(32) NOT NULL,
  fund_code VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  ledger_account_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_fund_account_no UNIQUE (account_no),
  CONSTRAINT uk_fund_account_ledger UNIQUE (ledger_account_id)
);

CREATE TABLE IF NOT EXISTS user_fund_account (
  id BIGINT NOT NULL PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  fund_code VARCHAR(16) NOT NULL,
  account_no VARCHAR(32) NOT NULL,
  CONSTRAINT uk_user_fund_account UNIQUE (user_id, fund_code),
  CONSTRAINT uk_user_fund_account_no UNIQUE (account_no)
);
