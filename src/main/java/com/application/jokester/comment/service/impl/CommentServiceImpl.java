package com.application.jokester.comment.service.impl;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.comment.dto.CommentResponse;
import com.application.jokester.comment.dto.CreateCommentRequest;
import com.application.jokester.comment.entity.Comment;
import com.application.jokester.comment.entity.CommentLike;
import com.application.jokester.comment.entity.CommentLikeId;
import com.application.jokester.comment.repository.CommentLikeRepository;
import com.application.jokester.comment.repository.CommentRepository;
import com.application.jokester.comment.service.CommentService;
import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.exception.UnauthorizedActionException;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final JokeRepository jokeRepository;
    private final UserRepository userRepository;

    // Adds a top-level comment to a joke.
    // Any authenticated user can comment on any joke — including their own.
    @Override
    @Transactional
    public CommentResponse addComment(UUID jokeId, CreateCommentRequest request, UUID userId) {
        Joke joke = jokeRepository.findById(jokeId)
                .orElseThrow(() -> new ResourceNotFoundException("Joke not found: " + jokeId));

        User userRef = userRepository.getReferenceById(userId);

        Comment comment = Comment.builder()
                .joke(joke)
                .user(userRef)
                .content(request.getContent().trim())
                .build();

        return toResponse(commentRepository.save(comment), false);
    }

    // Retrieves all top-level comments for a joke ordered by most liked first.
    // Each top-level comment includes its replies ordered by creation time.
    // This is a public endpoint — no authentication needed.
    @Override
    public List<CommentResponse> getCommentsByJoke(UUID jokeId) {
        if (!jokeRepository.existsById(jokeId)) {
            throw new ResourceNotFoundException("Joke not found: " + jokeId);
        }

        List<Comment> topLevel = commentRepository.findTopLevelByJokeId(jokeId);

        return topLevel.stream().map(c -> {
            List<Comment> replies = commentRepository.findRepliesByParentId(c.getId());
            CommentResponse response = toResponse(c, false);
            response.setReplies(replies.stream()
                    .map(r -> toResponse(r, true))
                    .toList());
            response.setReplyCount(replies.size());
            return response;
        }).toList();
    }

    // Adds a reply to an existing top-level comment.
    // We only allow replying to top-level comments — not to replies themselves.
    // This keeps the thread structure simple and avoids infinite nesting.
    @Override
    @Transactional
    public CommentResponse replyToComment(UUID parentCommentId, CreateCommentRequest request, UUID userId) {
        Comment parent = commentRepository.findByIdWithJokeOwner(parentCommentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + parentCommentId));

        if (parent.getParent() != null) {
            throw new IllegalArgumentException("Cannot reply to a reply. Please reply to the top-level comment.");
        }

        User userRef = userRepository.getReferenceById(userId);

        Comment reply = Comment.builder()
                .joke(parent.getJoke())
                .user(userRef)
                .content(request.getContent().trim())
                .parent(parent)
                .build();

        return toResponse(commentRepository.save(reply), true);
    }

    // Deletes a comment permanently.
    // Two types of users can delete a comment:
    //   1. The comment author — can always delete their own comment
    //   2. The joke owner — can delete any comment on their joke (moderator role)
    // When a top-level comment is deleted, all its replies are deleted automatically
    // by the ON DELETE CASCADE constraint in the database.
    @Override
    @Transactional
    public void deleteComment(UUID commentId, UUID requesterId) {
        Comment comment = commentRepository.findByIdWithJokeOwner(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        boolean isCommentAuthor = comment.getUser().getId().equals(requesterId);
        boolean isJokeOwner = comment.getJoke().getUser().getId().equals(requesterId);

        if (!isCommentAuthor && !isJokeOwner) {
            throw new UnauthorizedActionException(
                    "You can only delete your own comments or comments on your jokes");
        }

        commentRepository.delete(comment);
    }

    // Toggles a like on a comment.
    // If the user has not liked the comment — adds a like and increments the count.
    // If the user already liked the comment — removes the like and decrements the count.
    // The likes_count column is updated atomically to prevent race conditions.
    @Override
    @Transactional
    public CommentResponse likeComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findByIdWithJokeOwner(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found: " + commentId));

        Optional<CommentLike> existing = commentLikeRepository
                .findByIdCommentIdAndIdUserId(commentId, userId);

        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            commentRepository.decrementLikes(commentId);
            comment.setLikesCount(comment.getLikesCount() - 1);
        } else {
            User userRef = userRepository.getReferenceById(userId);
            CommentLike like = CommentLike.builder()
                    .id(new CommentLikeId(commentId, userId))
                    .comment(comment)
                    .user(userRef)
                    .build();
            commentLikeRepository.save(like);
            commentRepository.incrementLikes(commentId);
            comment.setLikesCount(comment.getLikesCount() + 1);
        }

        return toResponse(comment, comment.getParent() != null);
    }

    private CommentResponse toResponse(Comment comment, boolean isReply) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postedBy(comment.getUser().getUsername())
                .likesCount(comment.getLikesCount())
                .createdAt(comment.getCreatedAt())
                .isReply(isReply)
                .build();
    }
}