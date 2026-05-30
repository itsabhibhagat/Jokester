package com.application.jokester.comment.repository;

import com.application.jokester.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.user
        WHERE c.joke.id = :jokeId
        AND c.parent IS NULL
        ORDER BY c.likesCount DESC, c.createdAt ASC
    """)
    List<Comment> findTopLevelByJokeId(@Param("jokeId") UUID jokeId);

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.user
        WHERE c.parent.id = :parentId
        ORDER BY c.createdAt ASC
    """)
    List<Comment> findRepliesByParentId(@Param("parentId") UUID parentId);

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.user
        JOIN FETCH c.joke j
        JOIN FETCH j.user
        WHERE c.id = :id
    """)
    Optional<Comment> findByIdWithJokeOwner(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likesCount = c.likesCount + 1 WHERE c.id = :commentId")
    void incrementLikes(@Param("commentId") UUID commentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likesCount = c.likesCount - 1 WHERE c.id = :commentId")
    void decrementLikes(@Param("commentId") UUID commentId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.joke.id = :jokeId")
    int countByJokeId(@Param("jokeId") UUID jokeId);
}