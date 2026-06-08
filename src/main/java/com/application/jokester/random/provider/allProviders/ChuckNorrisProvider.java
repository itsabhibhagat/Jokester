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
public class ChuckNorrisProvider implements JokeProvider {

    private final WebClient webClient;

    @Override
    public String getProviderName() {
        return "ChuckNorris";
    }

    @Override
    public RandomJokeResponse fetchJoke() {
        try {
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

        } catch (WebClientResponseException e) {
            log.error("ChuckNorris API error: {} {}", e.getStatusCode(), e.getMessage());
            return RandomJokeResponse.builder()
                    .joke("Chuck Norris API is currently unavailable")
                    .provider(getProviderName())
                    .build();

        } catch (WebClientRequestException e) {
            log.error("ChuckNorris API timed out: {}", e.getMessage());
            return RandomJokeResponse.builder()
                    .joke("Chuck Norris API timed out, please try again")
                    .provider(getProviderName())
                    .build();
        }
    }
}