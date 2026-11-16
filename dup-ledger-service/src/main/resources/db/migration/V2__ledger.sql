CREATE TABLE IF NOT EXISTS ledger_journal (
  id BIGINT NOT NULL PRIMARY KEY,
  trace_id VARCHAR(64) NOT NULL,
  reference VARCHAR(128) NOT NULL,
  CONSTRAINT uk_ledger_journal_reference UNIQUE (reference)
);

CREATE TABLE IF NOT EXISTS ledger_posting (
  id BIGINT NOT NULL PRIMARY KEY,
  journal_id BIGINT NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  side VARCHAR(16) NOT NULL,
  minor_units BIGINT NOT NULL,
  scale INT NOT NULL,
  currency VARCHAR(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS ledger_freeze (
  id BIGINT NOT NULL PRIMARY KEY,
  trace_id VARCHAR(64) NOT NULL,
  reference VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  minor_units BIGINT NOT NULL,
  scale INT NOT NULL,
  currency VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  CONSTRAINT uk_ledger_freeze_reference UNIQUE (reference)
);
