package com.application.jokester.joke.controller;

import com.application.jokester.auth.security.UserPrincipal;
import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.service.JokeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jokes")
@RequiredArgsConstructor
public class JokeController {

    private final JokeService jokeService;

    @PostMapping("/post")
    public ResponseEntity<JokeResponse> createJoke(
            @Valid @RequestBody CreateJokeRequest createJokeRequest,
            // ✅ Fixed: UserPrincipal (what filter actually sets) not User
            // then call .getUser() to get the actual User entity
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jokeService.createJoke(createJokeRequest, userPrincipal.getUser()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JokeResponse> getJoke(@PathVariable UUID id) {
        return ResponseEntity.ok(jokeService.getJokeById(id));
    }

    @GetMapping
    public ResponseEntity<Page<JokeResponse>> searchJokes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(jokeService.searchJokes(query, categoryId, page, size));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteJoke(
            @PathVariable UUID id,
            // ✅ Fixed: same — UserPrincipal not User
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        jokeService.deleteJoke(id, userPrincipal.getUser());
        return ResponseEntity.noContent().build();
    }
}