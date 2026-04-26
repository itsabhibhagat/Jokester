package com.application.jokester.random.service;

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

    // key = "jokeApi"  → cached separately
    // key = "ChuckNorris" → cached separately
    // Same provider = same key = cache hits correctly
    @Cacheable(value = "random-jokes", key = "#providerName")
    public RandomJokeResponse fromProvider(String providerName) {
        return providers.stream()
                .filter(p -> p.getProviderName().equalsIgnoreCase(providerName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Provider not found: " + providerName))
                .fetchJoke();
    }

    // First call: 500ms (hits external API), stored in Redis
    // Every call after: 1-2ms (from Redis, for 1 hour)
    @Cacheable(value = "random-jokes", key = "'any-random'")
    public RandomJokeResponse fromRandomProvider() {
        int index = new Random().nextInt(providers.size());
        return providers.get(index).fetchJoke();
    }

    // No caching needed — just returns provider names, no external call
    public List<String> getAvailableProviders() {
        return providers.stream()
                .map(JokeProvider::getProviderName)
                .toList();
    }
}