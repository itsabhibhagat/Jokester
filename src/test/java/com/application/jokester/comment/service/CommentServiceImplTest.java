package com.application.jokester.comment.service;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.comment.dto.CommentResponse;
import com.application.jokester.comment.dto.CreateCommentRequest;
import com.application.jokester.comment.entity.Comment;
import com.application.jokester.comment.entity.CommentLike;
import com.application.jokester.comment.repository.CommentLikeRepository;
import com.application.jokester.comment.repository.CommentRepository;
import com.application.jokester.comment.service.impl.CommentServiceImpl;
import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.exception.UnauthorizedActionException;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private JokeRepository jokeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    void should_AddComment_When_JokeExists() {
        UUID jokeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Nice joke!");

        Joke joke = new Joke();
        User user = new User();
        user.setUsername("user1");

        when(jokeRepository.findById(jokeId)).thenReturn(Optional.of(joke));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Comment savedComment = Comment.builder()
                .id(UUID.randomUUID())
                .content("Nice joke!")
                .user(user)
                .joke(joke)
                .build();
        
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentResponse res = commentService.addComment(jokeId, req, userId);

        assertThat(res.getContent()).isEqualTo("Nice joke!");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void should_ThrowResourceNotFound_When_JokeNotExistsForComment() {
        UUID jokeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Nice joke!");

        when(jokeRepository.findById(jokeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(jokeId, req, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_ReplyToComment_When_TopLevelComment() {
        UUID parentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Reply!");

        User user = new User();
        user.setUsername("user1");

        Comment parent = Comment.builder().id(parentId).user(user).build();

        when(commentRepository.findByIdWithJokeOwner(parentId)).thenReturn(Optional.of(parent));
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        
        Comment savedReply = Comment.builder()
                .id(UUID.randomUUID())
                .content("Reply!")
                .user(user)
                .parent(parent)
                .build();
                
        when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);

        CommentResponse res = commentService.replyToComment(parentId, req, userId);

        assertThat(res.getContent()).isEqualTo("Reply!");
        assertThat(res.isReply()).isTrue();
    }

    @Test
    void should_ThrowIllegalArgument_When_ReplyingToReply() {
        UUID parentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CreateCommentRequest req = new CreateCommentRequest();

        Comment grandparent = Comment.builder().build();
        Comment parent = Comment.builder().parent(grandparent).build();

        when(commentRepository.findByIdWithJokeOwner(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> commentService.replyToComment(parentId, req, userId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_DeleteComment_When_CommentAuthor() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User author = new User();
        author.setId(userId);
        
        User jokeOwner = new User();
        jokeOwner.setId(UUID.randomUUID());
        
        Joke joke = new Joke();
        joke.setUser(jokeOwner);
        
        Comment comment = Comment.builder().id(commentId).user(author).joke(joke).build();

        when(commentRepository.findByIdWithJokeOwner(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(commentId, userId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void should_DeleteComment_When_JokeOwner() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User author = new User();
        author.setId(UUID.randomUUID());
        
        User jokeOwner = new User();
        jokeOwner.setId(userId);
        
        Joke joke = new Joke();
        joke.setUser(jokeOwner);
        
        Comment comment = Comment.builder().id(commentId).user(author).joke(joke).build();

        when(commentRepository.findByIdWithJokeOwner(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(commentId, userId);

        verify(commentRepository).delete(comment);
    }

    @Test
    void should_ThrowUnauthorized_When_NotAuthorizedToDelete() {
        UUID commentId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();

        User author = new User();
        author.setId(UUID.randomUUID());
        
        User jokeOwner = new User();
        jokeOwner.setId(UUID.randomUUID());
        
        Joke joke = new Joke();
        joke.setUser(jokeOwner);
        
        Comment comment = Comment.builder().id(commentId).user(author).joke(joke).build();

        when(commentRepository.findByIdWithJokeOwner(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(commentId, requesterId))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void should_LikeComment_When_NotLikedYet() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUsername("u");
        Comment comment = Comment.builder().id(commentId).likesCount(0).user(user).build();

        when(commentRepository.findByIdWithJokeOwner(commentId)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByIdCommentIdAndIdUserId(commentId, userId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(new User());

        commentService.likeComment(commentId, userId);

        verify(commentLikeRepository).save(any(CommentLike.class));
        verify(commentRepository).incrementLikes(commentId);
        assertThat(comment.getLikesCount()).isEqualTo(1);
    }

    @Test
    void should_UnlikeComment_When_AlreadyLiked() {
        UUID commentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setUsername("u");
        Comment comment = Comment.builder().id(commentId).likesCount(1).user(user).build();
        CommentLike like = CommentLike.builder().build();

        when(commentRepository.findByIdWithJokeOwner(commentId)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.findByIdCommentIdAndIdUserId(commentId, userId)).thenReturn(Optional.of(like));

        commentService.likeComment(commentId, userId);

        verify(commentLikeRepository).delete(like);
        verify(commentRepository).decrementLikes(commentId);
        assertThat(comment.getLikesCount()).isEqualTo(0);
    }
}
