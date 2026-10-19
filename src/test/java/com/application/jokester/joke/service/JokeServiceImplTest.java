package com.application.jokester.joke.service;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.category.entity.Category;
import com.application.jokester.category.repository.CategoryRepository;
import com.application.jokester.comment.repository.CommentRepository;
import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.exception.UnauthorizedActionException;
import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import com.application.jokester.joke.service.impl.JokeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JokeServiceImplTest {

    @Mock
    private JokeRepository jokeRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private JokeServiceImpl jokeService;

    @Test
    void should_CreateJoke_When_ValidRequest() {
        CreateJokeRequest request = new CreateJokeRequest();
        request.setTitle("Funny Joke");
        request.setContent("Joke content");
        request.setCategoryName("Pun");

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("joker");

        Category category = new Category();
        category.setName("Pun");

        Joke savedJoke = Joke.builder()
                .id(UUID.randomUUID())
                .title("Funny Joke")
                .content("Joke content")
                .category(category)
                .user(user)
                .upvotes(0)
                .downvotes(0)
                .build();

        when(categoryRepository.findByName("Pun")).thenReturn(Optional.of(category));
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(jokeRepository.save(any(Joke.class))).thenReturn(savedJoke);
        when(commentRepository.countByJokeId(savedJoke.getId())).thenReturn(0);

        JokeResponse response = jokeService.createJoke(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Funny Joke");
        verify(jokeRepository).save(any(Joke.class));
    }

    @Test
    void should_ThrowResourceNotFound_When_CategoryNotExist() {
        CreateJokeRequest request = new CreateJokeRequest();
        request.setTitle("Funny Joke");
        request.setContent("Joke content");
        request.setCategoryName("Unknown");

        UUID userId = UUID.randomUUID();

        when(categoryRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jokeService.createJoke(request, userId));
        verify(jokeRepository, never()).save(any(Joke.class));
    }

    @Test
    void should_GetJokeById_When_Exists() {
        UUID jokeId = UUID.randomUUID();

        User user = new User();
        user.setUsername("joker");

        Category category = new Category();
        category.setName("Pun");

        Joke joke = Joke.builder()
                .id(jokeId)
                .title("Funny Joke")
                .content("Joke content")
                .category(category)
                .user(user)
                .upvotes(10)
                .downvotes(2)
                .build();

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.of(joke));
        when(commentRepository.countByJokeId(jokeId)).thenReturn(5);

        JokeResponse response = jokeService.getJokeById(jokeId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(jokeId);
        assertThat(response.getCommentCount()).isEqualTo(5);
    }

    @Test
    void should_ThrowResourceNotFound_When_JokeNotExists() {
        UUID jokeId = UUID.randomUUID();

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> jokeService.getJokeById(jokeId));
    }

    @Test
    void should_DeleteJoke_When_OwnerDeletes() {
        UUID jokeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Joke joke = Joke.builder()
                .id(jokeId)
                .user(user)
                .build();

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.of(joke));

        jokeService.deleteJoke(jokeId, userId);

        verify(jokeRepository).delete(joke);
    }

    @Test
    void should_ThrowUnauthorized_When_NonOwnerDeletes() {
        UUID jokeId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        User owner = new User();
        owner.setId(ownerId);

        Joke joke = Joke.builder()
                .id(jokeId)
                .user(owner)
                .build();

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.of(joke));

        assertThrows(UnauthorizedActionException.class, () -> jokeService.deleteJoke(jokeId, otherUserId));
        verify(jokeRepository, never()).delete(any(Joke.class));
    }
}
