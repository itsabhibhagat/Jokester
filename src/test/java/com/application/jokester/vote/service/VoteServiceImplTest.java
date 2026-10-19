package com.application.jokester.vote.service;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import com.application.jokester.vote.entity.Vote;
import com.application.jokester.vote.entity.VoteType;
import com.application.jokester.vote.repository.VoteRepository;
import com.application.jokester.vote.service.impl.VoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @Mock
    private VoteRepository voteRepository;
    @Mock
    private JokeRepository jokeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private VoteServiceImpl voteService;

    private Joke createMockJoke(UUID jokeId) {
        Joke joke = mock(Joke.class);
        when(joke.getId()).thenReturn(jokeId);
        
        com.application.jokester.category.entity.Category cat = mock(com.application.jokester.category.entity.Category.class);
        when(cat.getName()).thenReturn("Pun");
        when(joke.getCategory()).thenReturn(cat);
        
        User user = new User();
        user.setUsername("testuser");
        when(joke.getUser()).thenReturn(user);
        
        return joke;
    }

    @Test
    void should_ApplyUpvote_When_NoExistingVote() {
        UUID jokeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Joke joke = createMockJoke(jokeId);

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.of(joke));
        when(voteRepository.findByUserIdAndJokeId(userId, jokeId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(new User());

        voteService.vote(jokeId, VoteType.UP, userId);

        verify(jokeRepository).incrementUpvotes(jokeId);
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    void should_ApplyDownvote_When_NoExistingVote() {
        UUID jokeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Joke joke = createMockJoke(jokeId);

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.of(joke));
        when(voteRepository.findByUserIdAndJokeId(userId, jokeId)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(new User());

        voteService.vote(jokeId, VoteType.DOWN, userId);

        verify(jokeRepository).incrementDownvotes(jokeId);
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    void should_UndoVote_When_SameVoteTypeAgain() {
        UUID jokeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Joke joke = createMockJoke(jokeId);

        Vote existingVote = Vote.builder().voteType(VoteType.UP).build();

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.of(joke));
        when(voteRepository.findByUserIdAndJokeId(userId, jokeId)).thenReturn(Optional.of(existingVote));

        voteService.vote(jokeId, VoteType.UP, userId);

        verify(jokeRepository).decrementUpvotes(jokeId);
        verify(voteRepository).delete(existingVote);
    }

    @Test
    void should_SwitchVote_When_DifferentVoteType() {
        UUID jokeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Joke joke = createMockJoke(jokeId);

        Vote existingVote = Vote.builder().voteType(VoteType.UP).build();

        when(jokeRepository.findByIdWithDetails(jokeId)).thenReturn(Optional.of(joke));
        when(voteRepository.findByUserIdAndJokeId(userId, jokeId)).thenReturn(Optional.of(existingVote));

        voteService.vote(jokeId, VoteType.DOWN, userId);

        verify(jokeRepository).decrementUpvotes(jokeId);
        verify(jokeRepository).incrementDownvotes(jokeId);
        verify(voteRepository).save(existingVote);
        assertThat(existingVote.getVoteType()).isEqualTo(VoteType.DOWN);
    }
}
