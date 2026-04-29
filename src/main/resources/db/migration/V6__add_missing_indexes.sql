-- GIN index for full-text search on the search_vec column.
-- Without this index, every search query does a full sequential scan of all 2M rows.
-- With this index, PostgreSQL jumps directly to matching rows in under 5ms.
CREATE INDEX IF NOT EXISTS idx_jokes_search
    ON jokes USING GIN(search_vec);

-- Index for sorting by upvotes descending.
-- Used in the default sort order for all joke listing endpoints.
-- Without this, PostgreSQL sorts 2M rows on every request.
CREATE INDEX IF NOT EXISTS idx_jokes_upvotes_desc
    ON jokes(upvotes DESC);

-- Index on user_id foreign key for fast user joke lookups.
-- Used by the getJokesByUsername endpoint.
CREATE INDEX IF NOT EXISTS idx_jokes_user_id
    ON jokes(user_id);