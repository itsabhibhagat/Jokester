package com.application.jokester.joke.service.impl;

import com.application.jokester.auth.entity.User;
import com.application.jokester.category.entity.Category;
import com.application.jokester.category.repository.CategoryRepository;
import com.application.jokester.config.datasource.ReadOnly;
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
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(()->new RuntimeException("Category not found"));

        Joke joke = Joke.builder()
                .user(currentUser)
                .category(category)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        return toResponse(jokeRepository.save(joke));
    }

    @Override
    @ReadOnly
    @Transactional(readOnly = true)
    @Cacheable(value = "joke", key = "#id")
    public JokeResponse getJokeById(UUID id){
        return jokeRepository.findByIdWithDetails(id)
                .map(this::toResponse)
                .orElseThrow(()-> new RuntimeException("Joke Not found"));
    }

    @Override
    @ReadOnly
    @Transactional(readOnly = true)
    public Page<JokeResponse> searchJokes(String query, Integer categoryId, int page, int size){
        Pageable pageable = PageRequest.of(page, size);
        return jokeRepository
                .searchJokesOptimized(query, categoryId, pageable)  // ← Use new method
                .map(this::toResponse);
    }

    @Override
    @Transactional
    @CacheEvict(value = "joke", key = "#id")
    public void deleteJoke(UUID id, User currentUser){
        Joke joke = jokeRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Joke not found"));
        if(!joke.getUser().getId().equals(currentUser.getId())){
            throw new RuntimeException("User not the same user");
        }
        jokeRepository.delete(joke);
    }

    private JokeResponse toResponse(Joke joke) {
        return JokeResponse.builder()
                .id(joke.getId())
                .title((joke.getTitle()))
                .content(joke.getContent())
                .category(joke.getCategory().getName())
                .postedBy((joke.getUser().getUsername()))
                .upvotes(joke.getUpvotes())
                .downvotes(joke.getDownvotes())
                .createdAt(joke.getCreated_at())
                .build();
    }
}
