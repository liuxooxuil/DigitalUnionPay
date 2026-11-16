CREATE TABLE IF NOT EXISTS trade_fill (
  id BIGINT NOT NULL PRIMARY KEY,
  reference VARCHAR(128) NOT NULL,
  order_reference VARCHAR(128) NOT NULL,
  minor_units BIGINT NOT NULL,
  scale INT NOT NULL,
  currency VARCHAR(16) NOT NULL,
  CONSTRAINT uk_trade_fill_reference UNIQUE (reference)
);
