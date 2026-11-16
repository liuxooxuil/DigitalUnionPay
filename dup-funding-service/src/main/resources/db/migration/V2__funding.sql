CREATE TABLE IF NOT EXISTS funding_deposit (
  id BIGINT NOT NULL PRIMARY KEY,
  reference VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  minor_units BIGINT NOT NULL,
  scale INT NOT NULL,
  currency VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  CONSTRAINT uk_funding_deposit_reference UNIQUE (reference)
);

CREATE TABLE IF NOT EXISTS funding_withdrawal (
  id BIGINT NOT NULL PRIMARY KEY,
  reference VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  minor_units BIGINT NOT NULL,
  scale INT NOT NULL,
  currency VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  CONSTRAINT uk_funding_withdrawal_reference UNIQUE (reference)
);
