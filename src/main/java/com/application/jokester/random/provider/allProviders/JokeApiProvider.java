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
public class JokeApiProvider implements JokeProvider {

    private final WebClient webClient;

    @Override
    public String getProviderName() {
        return "jokeApi";
    }

    @Override
    @CircuitBreaker(name = "jokeApi", fallbackMethod = "fetchJokeFallback")
    @Retry(name = "jokeApi")
    public RandomJokeResponse fetchJoke() {
        var response = webClient.get()
                .uri("https://v2.jokeapi.dev/joke/Any?type=single")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String joke = (response != null && response.get("joke") != null)
                ? response.get("joke").toString()
                : "Could not fetch joke from JokeAPI";

        return RandomJokeResponse.builder()
                .joke(joke)
                .provider(getProviderName())
                .build();
    }

    @SuppressWarnings("unused")
    private RandomJokeResponse fetchJokeFallback(Throwable t) {
        log.warn("Circuit breaker fallback for JokeAPI: {}", t.getMessage());
        return RandomJokeResponse.builder()
                .joke("JokeAPI is currently unavailable. The circuit breaker is open.")
                .provider(getProviderName())
                .build();
    }
}