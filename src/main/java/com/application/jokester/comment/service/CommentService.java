package com.application.jokester.comment.service;

import com.application.jokester.comment.dto.CommentResponse;
import com.application.jokester.comment.dto.CreateCommentRequest;

import java.util.List;
import java.util.UUID;

public interface CommentService {

    CommentResponse addComment(UUID jokeId, CreateCommentRequest request, UUID userId);

    List<CommentResponse> getCommentsByJoke(UUID jokeId);

    CommentResponse replyToComment(UUID parentCommentId, CreateCommentRequest request, UUID userId);

    void deleteComment(UUID commentId, UUID requesterId);

    CommentResponse likeComment(UUID commentId, UUID userId);
}