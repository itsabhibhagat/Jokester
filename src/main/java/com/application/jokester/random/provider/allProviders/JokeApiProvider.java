package com.application.jokester.random.provider.allProviders;

import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.provider.JokeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    public RandomJokeResponse fetchJoke() {
        try {
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

        } catch (WebClientResponseException e) {
            log.error("JokeAPI returned error: {} {}", e.getStatusCode(), e.getMessage());
            return RandomJokeResponse.builder()
                    .joke("JokeAPI is currently unavailable")
                    .provider(getProviderName())
                    .build();

        } catch (WebClientRequestException e) {
            log.error("JokeAPI timed out: {}", e.getMessage());
            return RandomJokeResponse.builder()
                    .joke("JokeAPI timed out, please try again")
                    .provider(getProviderName())
                    .build();
        }
    }
}