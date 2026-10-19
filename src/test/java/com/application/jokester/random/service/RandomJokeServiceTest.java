package com.application.jokester.random.service;

import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.provider.JokeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RandomJokeServiceTest {

    @Mock
    private JokeProvider jokeApiProvider;

    @Mock
    private JokeProvider chuckNorrisProvider;

    private RandomJokeService randomJokeService;

    @BeforeEach
    void setUp() {
        lenient().when(jokeApiProvider.getProviderName()).thenReturn("jokeApi");
        lenient().when(chuckNorrisProvider.getProviderName()).thenReturn("ChuckNorris");
        randomJokeService = new RandomJokeService(List.of(jokeApiProvider, chuckNorrisProvider));
    }

    @Test
    void should_ReturnJoke_When_ProviderExists() {
        RandomJokeResponse mockResponse = RandomJokeResponse.builder().joke("Haha").provider("jokeApi").build();
        when(jokeApiProvider.fetchJoke()).thenReturn(mockResponse);

        RandomJokeResponse response = randomJokeService.fromProvider("jokeApi");

        assertThat(response).isNotNull();
        assertThat(response.getJoke()).isEqualTo("Haha");
        assertThat(response.getProvider()).isEqualTo("jokeApi");
        verify(jokeApiProvider).fetchJoke();
    }

    @Test
    void should_ThrowResourceNotFound_When_ProviderNotExists() {
        assertThatThrownBy(() -> randomJokeService.fromProvider("unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Provider not found: unknown");
    }

    @Test
    void should_ReturnJoke_When_RandomProviderCalled() {
        RandomJokeResponse mockResponse = RandomJokeResponse.builder().joke("Haha").provider("jokeApi").build();
        lenient().when(jokeApiProvider.fetchJoke()).thenReturn(mockResponse);
        lenient().when(chuckNorrisProvider.fetchJoke()).thenReturn(mockResponse);

        RandomJokeResponse response = randomJokeService.fromRandomProvider();

        assertThat(response).isNotNull();
        assertThat(response.getJoke()).isEqualTo("Haha");
    }

    @Test
    void should_ReturnProviderNames_When_GetAvailableProviders() {
        List<String> providers = randomJokeService.getAvailableProviders();

        assertThat(providers).hasSize(2);
        assertThat(providers).containsExactlyInAnyOrder("jokeApi", "ChuckNorris");
    }
}
