CREATE TABLE IF NOT EXISTS match_order (
  id BIGINT NOT NULL PRIMARY KEY,
  reference VARCHAR(128) NOT NULL,
  account_id VARCHAR(64) NOT NULL,
  side VARCHAR(16) NOT NULL,
  status VARCHAR(32) NOT NULL,
  CONSTRAINT uk_match_order_reference UNIQUE (reference)
);
