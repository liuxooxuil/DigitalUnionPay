CREATE TABLE IF NOT EXISTS dup_idempotency (
  scope VARCHAR(64) NOT NULL,
  idem_key VARCHAR(128) NOT NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (scope, idem_key)
);

CREATE TABLE IF NOT EXISTS dup_outbox (
  id CHAR(36) NOT NULL PRIMARY KEY,
  event_type VARCHAR(128) NOT NULL,
  payload VARCHAR(4000) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  attempts INT NOT NULL,
  created_at DATETIME NOT NULL,
  sent_at DATETIME NULL
);

CREATE TABLE IF NOT EXISTS dup_dead_letter (
  id CHAR(36) NOT NULL PRIMARY KEY,
  event_type VARCHAR(128) NOT NULL,
  payload VARCHAR(4000) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS dup_tcc_fence (
  branch_id VARCHAR(128) NOT NULL PRIMARY KEY,
  status VARCHAR(16) NOT NULL,
  updated_at DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS dup_service_nonce (
  nonce CHAR(36) NOT NULL PRIMARY KEY,
  seen_at DATETIME NOT NULL
);
