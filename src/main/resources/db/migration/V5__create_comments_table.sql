CREATE TABLE comments (
                          id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                          joke_id    UUID        NOT NULL REFERENCES jokes(id)  ON DELETE CASCADE,
                          user_id    UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
                          content    TEXT        NOT NULL,
                          created_at TIMESTAMP   DEFAULT NOW()
);

CREATE INDEX idx_comments_joke_id ON comments(joke_id);

CREATE INDEX idx_comments_user_id ON comments(user_id);