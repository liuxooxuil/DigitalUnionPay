CREATE TABLE IF NOT EXISTS ledger_account (
  id BIGINT NOT NULL PRIMARY KEY,
  account_id VARCHAR(64) NOT NULL,
  currency VARCHAR(16) NOT NULL,
  available BIGINT NOT NULL,
  frozen BIGINT NOT NULL,
  scale INT NOT NULL,
  version INT NOT NULL,
  owner_user_id VARCHAR(64) NULL,
  CONSTRAINT uk_ledger_account UNIQUE (account_id, currency)
);
