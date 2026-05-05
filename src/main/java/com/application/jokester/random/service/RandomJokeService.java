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

    // Single Random instance reused across calls — not created new each time.
    private static final Random RANDOM = new Random();

    @Cacheable(value = "random-jokes", key = "#providerName")
    public RandomJokeResponse fromProvider(String providerName) {
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found: " + providerName))
                .fetchJoke();
    }

    @Cacheable(value = "random-jokes", key = "'any-random'")
    public RandomJokeResponse fromRandomProvider() {
        return providers.get(RANDOM.nextInt(providers.size())).fetchJoke();
    }

    public List<String> getAvailableProviders() {
        return providers.stream().map(JokeProvider::getProviderName).toList();
    }
}