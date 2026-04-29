package com.application.jokester.random.service;

import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.provider.JokeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RandomJokeService {

    private final List<JokeProvider> providers;

    @Cacheable(value = "random-jokes", key = "#providerName")
    public RandomJokeResponse fromProvider(String providerName) {
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joke provider not found: '" + providerName +
                                "'. Available providers: " + getAvailableProviders()))
                .fetchJoke();
    }

    @Cacheable(value = "random-jokes", key = "'any-random'")
    public RandomJokeResponse fromRandomProvider() {
        int index = new Random().nextInt(providers.size());
        return providers.get(index).fetchJoke();
    }

    public List<String> getAvailableProviders() {
        return providers.stream()
                .map(JokeProvider::getProviderName)
                .toList();
    }
}