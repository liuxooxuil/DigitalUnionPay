CREATE TABLE IF NOT EXISTS risk_decision (
  id BIGINT NOT NULL PRIMARY KEY,
  reference VARCHAR(128) NOT NULL,
  action VARCHAR(32) NOT NULL,
  level VARCHAR(16) NOT NULL,
  created_at DATETIME NOT NULL,
  CONSTRAINT uk_risk_decision_reference UNIQUE (reference)
);
