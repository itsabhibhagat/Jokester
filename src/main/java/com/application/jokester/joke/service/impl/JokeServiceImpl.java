package com.application.jokester.joke.service.impl;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.category.entity.Category;
import com.application.jokester.category.repository.CategoryRepository;
import com.application.jokester.comment.repository.CommentRepository;
import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.exception.UnauthorizedActionException;
import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokePageResponse;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import com.application.jokester.joke.service.JokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JokeServiceImpl implements JokeService {

    private final JokeRepository jokeRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    @Override
    @Transactional
    public JokeResponse createJoke(CreateJokeRequest request, UUID userId) {
        Category category = categoryRepository
                .findByName(request.getCategoryName().trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category '" + request.getCategoryName() + "' does not exist."));

        User userRef = userRepository.getReferenceById(userId);

        Joke joke = Joke.builder()
                .user(userRef)
                .category(category)
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .build();

        return toResponse(jokeRepository.save(joke));
    }

    @Override
    @Cacheable(value = "joke", key = "#id")
    public JokeResponse getJokeById(UUID id) {
        return jokeRepository.findByIdWithDetails(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joke not found with id: " + id));
    }

    @Override
    public JokePageResponse searchJokes(String query, String categoryName,
                                        Integer lastUpvotes, UUID lastId, int size) {
        String normalizedQuery = (query != null && query.isBlank()) ? null : query;
        String normalizedCategory = (categoryName != null && categoryName.isBlank()) ? null : categoryName;
        String lastIdStr = (lastId != null) ? lastId.toString() : null;

        // Fetch one extra record so we can determine if there are more pages.
        List<Joke> jokes = jokeRepository.searchJokesKeyset(
                normalizedQuery, normalizedCategory, lastUpvotes, lastIdStr, size + 1);

        boolean hasMore = jokes.size() > size;
        List<Joke> page = hasMore ? jokes.subList(0, size) : jokes;
        List<JokeResponse> responses = page.stream().map(this::toResponse).toList();

        Integer nextUpvotes = hasMore ? page.get(page.size() - 1).getUpvotes() : null;
        UUID nextId = hasMore ? page.get(page.size() - 1).getId() : null;

        return JokePageResponse.builder()
                .jokes(responses)
                .size(responses.size())
                .hasMore(hasMore)
                .lastUpvotes(nextUpvotes)
                .lastId(nextId)
                .build();
    }

    @Override
    public Page<JokeResponse> getJokesByCategory(String categoryName, int page, int size) {
        return jokeRepository
                .findByCategoryName(categoryName, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    public Page<JokeResponse> getJokesByUsername(String username, int page, int size) {
        return jokeRepository
                .findByUsername(username, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "joke", key = "#id")
    public void deleteJoke(UUID id, UUID userId) {
        Joke joke = jokeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joke not found with id: " + id));

        if (!joke.getUser().getId().equals(userId)) {
            throw new UnauthorizedActionException(
                    "You are not authorized to delete this joke");
        }

        jokeRepository.delete(joke);
    }

    private JokeResponse toResponse(Joke joke) {
        return JokeResponse.builder()
                .id(joke.getId())
                .title(joke.getTitle())
                .content(joke.getContent())
                .category(joke.getCategory().getName())
                .postedBy(joke.getUser().getUsername())
                .upvotes(joke.getUpvotes())
                .downvotes(joke.getDownvotes())
                .commentCount(commentRepository.countByJokeId(joke.getId()))
                .createdAt(joke.getCreated_at())
                .build();
    }
}