package com.application.jokester.joke.service;

import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokePageResponse;
import com.application.jokester.joke.dto.JokeResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface JokeService {

    JokeResponse createJoke(CreateJokeRequest request, UUID userId);

    JokeResponse getJokeById(UUID id);

    JokePageResponse searchJokes(String query, String categoryName,
                                 Integer lastUpvotes, UUID lastId, int size);

    Page<JokeResponse> getJokesByCategory(String categoryName, int page, int size);

    Page<JokeResponse> getJokesByUsername(String username, int page, int size);

    void deleteJoke(UUID id, UUID userId);
}