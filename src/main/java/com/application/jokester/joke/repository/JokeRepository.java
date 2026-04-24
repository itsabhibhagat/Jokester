package com.application.jokester.joke.repository;

import com.application.jokester.joke.entity.Joke;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JokeRepository extends JpaRepository<Joke, UUID> {

    @Query("""
        SELECT j FROM Joke j
        WHERE (:query IS NULL OR
               LOWER(j.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(j.content) LIKE LOWER(CONCAT('%', :query, '%')))
        AND   (:categoryId IS NULL OR j.category.id = :categoryId)
        ORDER BY j.upvotes DESC
    """)
    Page<Joke> searchJokes(
            @Param("query") String query,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    @Query(value = """
        SELECT j.* FROM jokes j
        WHERE (:query IS NULL OR j.search_vec @@ plainto_tsquery('english', :query))
        AND (:categoryId IS NULL OR j.category_id = :categoryId)
        ORDER BY j.upvotes DESC
        LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}
    """,
            countQuery = """
        SELECT COUNT(*) FROM jokes j
        WHERE (:query IS NULL OR j.search_vec @@ plainto_tsquery('english', :query))
        AND (:categoryId IS NULL OR j.category_id = :categoryId)
    """,
            nativeQuery = true)
    Page<Joke> searchJokesOptimized(
            @Param("query") String query,
            @Param("categoryId") Integer categoryId,
            Pageable pageable
    );

    // 🔥 FIXED: add clearAutomatically + flushAutomatically

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

    @Query("""
    SELECT j FROM Joke j
    JOIN FETCH j.user
    JOIN FETCH j.category
    WHERE j.id = :id
""")
    Optional<Joke> findByIdWithDetails(@Param("id") UUID id);
}