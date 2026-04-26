package com.application.jokester.joke.repository;

import com.application.jokester.joke.entity.Joke;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JokeRepository extends JpaRepository<Joke, UUID> {

    // Searches jokes by text and optional category name.
    // Uses PostgreSQL full-text search via search_vec column for performance.
    // If no filters are provided, returns all jokes sorted by most upvotes.
    // The category name is matched case-insensitively.
    @Query(value = """
        SELECT j.* FROM jokes j
        JOIN categories c ON j.category_id = c.id
        WHERE (:query IS NULL OR j.search_vec @@ plainto_tsquery('english', :query))
        AND (:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName))
        ORDER BY j.upvotes DESC
        LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
    """,
            countQuery = """
        SELECT COUNT(*) FROM jokes j
        JOIN categories c ON j.category_id = c.id
        WHERE (:query IS NULL OR j.search_vec @@ plainto_tsquery('english', :query))
        AND (:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName))
    """,
            nativeQuery = true)
    Page<Joke> searchJokesOptimized(
            @Param("query") String query,
            @Param("categoryName") String categoryName,
            Pageable pageable
    );

    // Returns all jokes belonging to a specific category, sorted by upvotes.
    // Category name matching is case-insensitive so "animal jokes" and "Animal Jokes" both work.
    @Query("""
        SELECT j FROM Joke j
        JOIN FETCH j.user
        JOIN FETCH j.category
        WHERE LOWER(j.category.name) = LOWER(:categoryName)
        ORDER BY j.upvotes DESC
    """)
    Page<Joke> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    // Returns all jokes posted by a specific user, sorted by most recent first.
    // This allows viewing another user's joke history.
    @Query("""
        SELECT j FROM Joke j
        JOIN FETCH j.user
        JOIN FETCH j.category
        WHERE LOWER(j.user.username) = LOWER(:username)
        ORDER BY j.created_at DESC
    """)
    Page<Joke> findByUsername(@Param("username") String username, Pageable pageable);

    // Loads a joke along with its user and category in a single database query.
    // Without JOIN FETCH, accessing joke.getUser() or joke.getCategory()
    // would trigger separate database queries for each — very inefficient.
    @Query("""
        SELECT j FROM Joke j
        JOIN FETCH j.user
        JOIN FETCH j.category
        WHERE j.id = :id
    """)
    Optional<Joke> findByIdWithDetails(@Param("id") UUID id);

    // Atomic increment and decrement operations for vote counts.
    // These update only the vote count column directly in the database
    // without loading the entire Joke object, which is much faster
    // and prevents race conditions when multiple users vote simultaneously.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Joke j SET j.upvotes = j.upvotes + 1 WHERE j.id = :jokeId")
    void incrementUpvotes(@Param("jokeId") UUID jokeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Joke j SET j.upvotes = j.upvotes - 1 WHERE j.id = :jokeId")
    void decrementUpvotes(@Param("jokeId") UUID jokeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Joke j SET j.downvotes = j.downvotes + 1 WHERE j.id = :jokeId")
    void incrementDownvotes(@Param("jokeId") UUID jokeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Joke j SET j.downvotes = j.downvotes - 1 WHERE j.id = :jokeId")
    void decrementDownvotes(@Param("jokeId") UUID jokeId);
}