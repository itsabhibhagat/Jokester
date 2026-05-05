package com.application.jokester.joke.repository;

import com.application.jokester.joke.entity.Joke;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JokeRepository extends JpaRepository<Joke, UUID> {

    @Query(value = """
        SELECT j.* FROM jokes j
        JOIN categories c ON j.category_id = c.id
        WHERE (:query IS NULL OR j.search_vec @@ plainto_tsquery('english', :query))
        AND (:categoryName IS NULL OR LOWER(c.name) = LOWER(:categoryName))
        AND (
            CAST(:lastUpvotes AS INTEGER) IS NULL
            OR j.upvotes < :lastUpvotes
            OR (j.upvotes = :lastUpvotes AND j.id::text < :lastId)
        )
        ORDER BY j.upvotes DESC, j.id DESC
        LIMIT :size
    """, nativeQuery = true)
    List<Joke> searchJokesKeyset(
            @Param("query") String query,
            @Param("categoryName") String categoryName,
            @Param("lastUpvotes") Integer lastUpvotes,
            @Param("lastId") String lastId,
            @Param("size") int size
    );

    @Query("""
        SELECT j FROM Joke j
        JOIN FETCH j.user
        JOIN FETCH j.category
        WHERE LOWER(j.category.name) = LOWER(:categoryName)
        ORDER BY j.upvotes DESC
    """)
    Page<Joke> findByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

    @Query("""
        SELECT j FROM Joke j
        JOIN FETCH j.user
        JOIN FETCH j.category
        WHERE LOWER(j.user.username) = LOWER(:username)
        ORDER BY j.created_at DESC
    """)
    Page<Joke> findByUsername(@Param("username") String username, Pageable pageable);

    @Query("""
        SELECT j FROM Joke j
        JOIN FETCH j.user
        JOIN FETCH j.category
        WHERE j.id = :id
    """)
    Optional<Joke> findByIdWithDetails(@Param("id") UUID id);

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