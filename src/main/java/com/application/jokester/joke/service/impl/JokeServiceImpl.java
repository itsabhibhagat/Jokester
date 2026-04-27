package com.application.jokester.joke.service.impl;

import com.application.jokester.auth.entity.User;
import com.application.jokester.category.entity.Category;
import com.application.jokester.category.repository.CategoryRepository;
import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.exception.UnauthorizedActionException;
import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import com.application.jokester.joke.service.JokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JokeServiceImpl implements JokeService {

    private final JokeRepository jokeRepository;
    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public JokeResponse createJoke(CreateJokeRequest request, User currentUser) {
        String trimmedCategoryName = request.getCategoryName().trim();

        Category category = categoryRepository
                .findByName(trimmedCategoryName)
                .orElseGet(() -> categoryRepository.save(
                        Category.builder().name(trimmedCategoryName).build()
                ));

        Joke joke = Joke.builder()
                .user(currentUser)
                .category(category)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .build();

        return toResponse(jokeRepository.save(joke));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "joke", key = "#id")
    public JokeResponse getJokeById(UUID id) {
        return jokeRepository.findByIdWithDetails(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joke not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JokeResponse> searchJokes(String query, String categoryName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedQuery = (query != null && query.isBlank()) ? null : query;
        String normalizedCategory = (categoryName != null && categoryName.isBlank()) ? null : categoryName;
        return jokeRepository
                .searchJokesOptimized(normalizedQuery, normalizedCategory, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JokeResponse> getJokesByCategory(String categoryName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jokeRepository
                .findByCategoryName(categoryName, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<JokeResponse> getJokesByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jokeRepository
                .findByUsername(username, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "joke", key = "#id")
    public void deleteJoke(UUID id, User currentUser) {
        Joke joke = jokeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joke not found with id: " + id));

        if (!joke.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedActionException(
                    "You are not authorized to delete this joke");
        }

        jokeRepository.delete(joke);
    }

    // Converts a Joke database entity to a JokeResponse DTO.
    // DTO contains only what the client needs — no internal IDs or foreign keys.
    private JokeResponse toResponse(Joke joke) {
        return JokeResponse.builder()
                .id(joke.getId())
                .title(joke.getTitle())
                .content(joke.getContent())
                .category(joke.getCategory().getName())
                .postedBy(joke.getUser().getUsername())
                .upvotes(joke.getUpvotes())
                .downvotes(joke.getDownvotes())
                .createdAt(joke.getCreated_at())
                .build();
    }
}