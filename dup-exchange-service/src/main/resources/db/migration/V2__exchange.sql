CREATE TABLE IF NOT EXISTS exchange_lock (
  id BIGINT NOT NULL PRIMARY KEY,
  reference VARCHAR(128) NOT NULL,
  status VARCHAR(32) NOT NULL,
  CONSTRAINT uk_exchange_lock_reference UNIQUE (reference)
);
