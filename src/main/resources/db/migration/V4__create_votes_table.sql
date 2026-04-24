CREATE TABLE votes (
                       user_id   UUID        NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
                       joke_id   UUID        NOT NULL REFERENCES jokes(id)  ON DELETE CASCADE,
                       vote_type VARCHAR(10) NOT NULL CHECK (vote_type IN ('UP', 'DOWN')),
                       created_at TIMESTAMP DEFAULT NOW(),
                       PRIMARY KEY (user_id, joke_id)   -- one vote per user per joke
);