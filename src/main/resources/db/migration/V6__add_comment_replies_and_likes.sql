-- Adds replies support by adding a self-referencing foreign key on comments.
-- A top-level comment has parent_comment_id = NULL.
-- A reply has parent_comment_id pointing to the parent comment's id.
ALTER TABLE comments
    ADD COLUMN parent_comment_id UUID REFERENCES comments(id) ON DELETE CASCADE;

-- Tracks the total likes per comment for fast sorting without COUNT queries.
-- Updated atomically when a user likes or unlikes a comment.
ALTER TABLE comments
    ADD COLUMN likes_count INT NOT NULL DEFAULT 0;

-- Stores which user liked which comment.
-- Composite primary key enforces one like per user per comment at database level.
CREATE TABLE comment_likes (
                               comment_id UUID NOT NULL REFERENCES comments(id) ON DELETE CASCADE,
                               user_id    UUID NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
                               created_at TIMESTAMP DEFAULT NOW(),
                               PRIMARY KEY (comment_id, user_id)
);

-- Index for loading all replies of a parent comment.
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);

-- Composite index for loading all top-level comments of a joke sorted by most liked.
CREATE INDEX idx_comments_joke_likes ON comments(joke_id, likes_count DESC);

-- Index for the comment likes table for fast lookups.
CREATE INDEX idx_comment_likes_comment_id ON comment_likes(comment_id);