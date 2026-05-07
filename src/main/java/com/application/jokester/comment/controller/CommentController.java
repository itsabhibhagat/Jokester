package com.application.jokester.comment.controller;

import com.application.jokester.comment.dto.CommentResponse;
import com.application.jokester.comment.dto.CreateCommentRequest;
import com.application.jokester.comment.service.CommentService;
import com.application.jokester.common.ApiResponse;
import com.application.jokester.config.TokenPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Endpoints for commenting, replying, and liking comments on jokes")
public class CommentController {

    private final CommentService commentService;

    // Adds a new top-level comment to a joke.
    // Any authenticated user can comment on any joke.
    @Operation(
            summary = "Add a comment to a joke",
            description = "Posts a top-level comment on a joke. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/jokes/{jokeId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID jokeId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully",
                        commentService.addComment(jokeId, request, principal.userId())));
    }

    // Returns all top-level comments for a joke with their replies included.
    // Comments are sorted by most liked first.
    // Replies within each comment are sorted by oldest first.
    // This is a public endpoint — no authentication required.
    @Operation(
            summary = "Get comments for a joke",
            description = "Returns all comments for a joke ordered by most liked. Each comment includes its replies ordered by oldest first."
    )
    @GetMapping("/api/jokes/{jokeId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable UUID jokeId) {

        return ResponseEntity.ok(
                ApiResponse.success("Comments fetched successfully",
                        commentService.getCommentsByJoke(jokeId)));
    }

    // Replies to an existing top-level comment.
    // Replies can only be added to top-level comments — not to other replies.
    // Both the joke owner and any user can reply to any comment.
    @Operation(
            summary = "Reply to a comment",
            description = "Adds a reply to a top-level comment. You cannot reply to a reply. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> replyToComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reply added successfully",
                        commentService.replyToComment(commentId, request, principal.userId())));
    }

    // Deletes a comment or reply permanently.
    // The comment author can delete their own comment.
    // The joke owner can delete any comment on their joke.
    // Deleting a top-level comment also deletes all its replies.
    @Operation(
            summary = "Delete a comment",
            description = "Deletes a comment. The comment author or the joke owner can delete it. Deleting a top-level comment removes all its replies too. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal TokenPrincipal principal) {

        commentService.deleteComment(commentId, principal.userId());
        return ResponseEntity.ok(
                ApiResponse.success("Comment deleted successfully", null));
    }

    // Toggles a like on a comment.
    // First call: adds a like and returns updated comment with incremented likes_count.
    // Second call (same user): removes the like and returns updated comment with decremented count.
    @Operation(
            summary = "Like or unlike a comment",
            description = "Toggles a like on a comment. Call once to like, call again to unlike. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/api/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<CommentResponse>> likeComment(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal TokenPrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success("Like toggled successfully",
                        commentService.likeComment(commentId, principal.userId())));
    }
}