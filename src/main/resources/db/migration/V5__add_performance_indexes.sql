-- NEW FILE: src/main/resources/db/migration/V5__add_performance_indexes.sql

-- Composite index for category + upvotes (used in filtered search)
CREATE INDEX idx_jokes_category_upvotes ON jokes(category_id, upvotes DESC);

-- Index on created_at for "latest jokes" queries
CREATE INDEX idx_jokes_created_at ON jokes(created_at DESC);

-- Index on votes composite key (automatically created by PRIMARY KEY, but verify)
-- No action needed - already covered by PRIMARY KEY (user_id, joke_id)