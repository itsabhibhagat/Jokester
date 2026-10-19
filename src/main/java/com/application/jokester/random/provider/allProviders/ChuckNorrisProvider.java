package com.application.jokester.random.provider.allProviders;

import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.provider.JokeProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChuckNorrisProvider implements JokeProvider {

    private final WebClient webClient;

    @Override
    public String getProviderName() {
        return "ChuckNorris";
    }

    @Override
    @CircuitBreaker(name = "chuckNorrisApi", fallbackMethod = "fetchJokeFallback")
    @Retry(name = "chuckNorrisApi")
    public RandomJokeResponse fetchJoke() {
        var response = webClient.get()
                .uri("https://api.chucknorris.io/jokes/random")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String joke = (response != null && response.get("value") != null)
                ? response.get("value").toString()
                : "Could not fetch Chuck Norris joke";

        return RandomJokeResponse.builder()
                .joke(joke)
                .provider(getProviderName())
                .build();
    }

    @SuppressWarnings("unused")
    private RandomJokeResponse fetchJokeFallback(Throwable t) {
        log.warn("Circuit breaker fallback for ChuckNorris API: {}", t.getMessage());
        return RandomJokeResponse.builder()
                .joke("Chuck Norris API is currently unavailable. The circuit breaker is open.")
                .provider(getProviderName())
                .build();
    }
}