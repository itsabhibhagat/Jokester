CREATE TABLE jokes (
                       id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       category_id INT  NOT NULL REFERENCES categories(id),
                       title       VARCHAR(200) NOT NULL,
                       content     TEXT NOT NULL,
                       upvotes     INT  DEFAULT 0,
                       downvotes   INT  DEFAULT 0,
                       search_vec  TSVECTOR,
                       created_at  TIMESTAMP DEFAULT NOW()
);

-- Index for full-text search
CREATE INDEX idx_jokes_search   ON jokes USING GIN(search_vec);

-- Index for sorting by upvotes (used in search API default)
CREATE INDEX idx_jokes_upvotes  ON jokes(upvotes DESC);

-- Index for category browsing
CREATE INDEX idx_jokes_category ON jokes(category_id);

-- Auto-update search_vec when title or content changes
CREATE OR REPLACE FUNCTION update_joke_search_vec()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vec := to_tsvector('english',
        COALESCE(NEW.title, '') || ' ' || COALESCE(NEW.content, ''));
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_joke_search_vec
    BEFORE INSERT OR UPDATE ON jokes
                         FOR EACH ROW EXECUTE FUNCTION update_joke_search_vec();