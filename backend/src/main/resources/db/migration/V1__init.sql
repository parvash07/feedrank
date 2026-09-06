CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS feed_items (
  id BIGSERIAL PRIMARY KEY,
  external_id VARCHAR(64) NOT NULL UNIQUE,
  title TEXT NOT NULL,
  url TEXT,
  source VARCHAR(32) NOT NULL DEFAULT 'hackernews',
  author VARCHAR(128),
  score INT NOT NULL DEFAULT 0,
  tags TEXT NOT NULL DEFAULT '',
  summary TEXT,
  embedding_text TEXT,
  embedding_vector vector(768),
  hn_time BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_feed_items_created_at ON feed_items (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feed_items_tags ON feed_items USING gin (tags gin_trgm_ops);

CREATE TABLE IF NOT EXISTS interactions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  item_id BIGINT NOT NULL REFERENCES feed_items(id) ON DELETE CASCADE,
  interaction_type VARCHAR(16) NOT NULL,
  dwell_time_ms BIGINT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT chk_interaction_type CHECK (interaction_type IN ('click','upvote','skip','dwell'))
);
CREATE INDEX IF NOT EXISTS idx_interactions_user_created ON interactions (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_interactions_item ON interactions (item_id);

CREATE TABLE IF NOT EXISTS preference_weights (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  tag VARCHAR(64) NOT NULL,
  weight DOUBLE PRECISION NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, tag)
);

CREATE TABLE IF NOT EXISTS user_preference_vectors (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  vector_dim INT NOT NULL DEFAULT 768,
  vector_text TEXT NOT NULL DEFAULT '[]',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS feed_impressions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  item_id BIGINT NOT NULL REFERENCES feed_items(id) ON DELETE CASCADE,
  is_exploration BOOLEAN NOT NULL DEFAULT FALSE,
  reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_impressions_user_created ON feed_impressions (user_id, created_at DESC);
