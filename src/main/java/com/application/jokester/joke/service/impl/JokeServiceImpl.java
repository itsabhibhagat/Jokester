package com.application.jokester.joke.service.impl;

import com.application.jokester.auth.entity.User;
import com.application.jokester.category.entity.Category;
import com.application.jokester.category.repository.CategoryRepository;
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

    // Creates a new joke for the logged-in user.
    // The user provides a category name instead of a category ID.
    // If the category name already exists in the database, it is reused.
    // If the category does not exist, it is automatically created as a new category.
    // This means users are not limited to predefined categories — they can create new ones.
    @Override
    @Transactional
    public JokeResponse createJoke(CreateJokeRequest request, User currentUser) {
        String trimmedCategoryName = request.getCategoryName().trim();

        Category category = categoryRepository
                .findByName(trimmedCategoryName)
                .orElseGet(() -> {
                    Category newCategory = Category.builder()
                            .name(trimmedCategoryName)
                            .build();
                    return categoryRepository.save(newCategory);
                });

        Joke joke = Joke.builder()
                .user(currentUser)
                .category(category)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .build();

        return toResponse(jokeRepository.save(joke));
    }

    // Retrieves a single joke by its unique ID.
    // Results are cached in Redis so repeated requests for the same joke
    // do not hit the database — they are served from memory in under 2ms.
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "joke", key = "#id")
    public JokeResponse getJokeById(UUID id) {
        return jokeRepository.findByIdWithDetails(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Joke not found with id: " + id));
    }

    // Searches jokes by text keyword and optional category name.
    // Uses PostgreSQL full-text search for fast results even with millions of records.
    // Results are always sorted by highest upvotes so the best jokes appear first.
    // If no filters are provided, returns all jokes sorted by upvotes.
    // Results are paginated — default is 10 jokes per page.
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

    // Returns all jokes that belong to a specific category.
    // Category matching is case-insensitive so "animal jokes" and "Animal Jokes" are the same.
    // Results are sorted by upvotes so the most popular jokes in that category come first.
    @Override
    @Transactional(readOnly = true)
    public Page<JokeResponse> getJokesByCategory(String categoryName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jokeRepository
                .findByCategoryName(categoryName, pageable)
                .map(this::toResponse);
    }

    // Returns all jokes posted by a specific username.
    // This allows viewing another user's entire joke history.
    // Results are sorted by most recent first so the newest jokes appear at the top.
    @Override
    @Transactional(readOnly = true)
    public Page<JokeResponse> getJokesByUsername(String username, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jokeRepository
                .findByUsername(username, pageable)
                .map(this::toResponse);
    }

    // Deletes a joke permanently from the database.
    // Only the user who originally posted the joke can delete it.
    // If someone else tries to delete it, they get an authorization error.
    // The cache entry for this joke is also removed so stale data is not served.
    @Override
    @Transactional
    @CacheEvict(value = "joke", key = "#id")
    public void deleteJoke(UUID id, User currentUser) {
        Joke joke = jokeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Joke not found with id: " + id));
        if (!joke.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You are not authorized to delete this joke");
        }
        jokeRepository.delete(joke);
    }

    // Converts a Joke database entity into a JokeResponse DTO.
    // DTOs are what we send to the client — they contain only the fields
    // the client needs and no internal database details like foreign keys.
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