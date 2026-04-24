CREATE TABLE categories (
                            id   SERIAL PRIMARY KEY,
                            name VARCHAR(100) UNIQUE NOT NULL
);

-- Seed default categories from requirements
INSERT INTO categories (name) VALUES
                                  ('Animal Jokes'),
                                  ('Family Jokes');