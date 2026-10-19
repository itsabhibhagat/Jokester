package com.application.jokester.random.provider;

import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.provider.allProviders.ChuckNorrisProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChuckNorrisProviderTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ChuckNorrisProvider chuckNorrisProvider;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() {
        // We will mock the webClient chain in individual tests since some tests might not need it
        // Or we can mock it here if needed for all tests except simple ones.
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void should_ReturnJoke_When_ApiReturnsSuccess() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Map.class)).thenReturn(Mono.just(Map.of("value", "funny chuck norris joke")));

        RandomJokeResponse response = chuckNorrisProvider.fetchJoke();

        assertEquals("funny chuck norris joke", response.getJoke());
        assertEquals("ChuckNorris", response.getProvider());
    }

    @Test
    void should_ReturnProviderName() {
        assertEquals("ChuckNorris", chuckNorrisProvider.getProviderName());
    }
}
