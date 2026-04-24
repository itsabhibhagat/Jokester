package com.application.jokester.joke.service;

import com.application.jokester.auth.entity.User;
import com.application.jokester.category.entity.Category;
import com.application.jokester.category.repository.CategoryRepository;
import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

public interface JokeService {

    public JokeResponse createJoke(CreateJokeRequest request, User currentUser);

    public JokeResponse getJokeById(UUID id);

    public Page<JokeResponse> searchJokes(String query, Integer categoryId,
                                          int page, int size);

    public void deleteJoke(UUID id, User currentUser);

}
