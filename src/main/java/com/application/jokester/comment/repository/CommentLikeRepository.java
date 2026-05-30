package com.application.jokester.comment.repository;

import com.application.jokester.comment.entity.CommentLike;
import com.application.jokester.comment.entity.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

    Optional<CommentLike> findByIdCommentIdAndIdUserId(UUID commentId, UUID userId);
}