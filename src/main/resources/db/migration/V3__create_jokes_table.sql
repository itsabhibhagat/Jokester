CREATE TABLE jokes (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       category_id INT NOT NULL REFERENCES categories(id),
                       title VARCHAR(200) NOT NULL,
                       content TEXT NOT NULL,
                       upvotes INT DEFAULT 0,
                       downvotes INT DEFAULT 0,
                       search_vec TSVECTOR,
                       created_at TIMESTAMP DEFAULT NOW()
);

-- FULL-TEXT SEARCH (most important)
CREATE INDEX idx_jokes_search
    ON jokes USING GIN(search_vec);

-- CATEGORY + SORT (composite index)
CREATE INDEX idx_jokes_category_upvotes
    ON jokes(category_id, upvotes DESC);

-- GLOBAL SORT
CREATE INDEX idx_jokes_upvotes_desc
    ON jokes(upvotes DESC);

-- LATEST JOKES
CREATE INDEX idx_jokes_created_at
    ON jokes(created_at DESC);

-- USER LOOKUP
CREATE INDEX idx_jokes_user_id
    ON jokes(user_id);

-- AUTO UPDATE search_vec
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
                         FOR EACH ROW
                         EXECUTE FUNCTION update_joke_search_vec();