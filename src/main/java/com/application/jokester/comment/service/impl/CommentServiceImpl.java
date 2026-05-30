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