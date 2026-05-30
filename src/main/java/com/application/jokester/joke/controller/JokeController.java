package com.application.jokester.joke.controller;

import com.application.jokester.common.ApiResponse;
import com.application.jokester.config.TokenPrincipal;
import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokePageResponse;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.service.JokeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Jokes", description = "Endpoints for creating, searching, and managing jokes")
public class JokeController {

    private final JokeService jokeService;

    @Operation(summary = "Post a new joke", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/post")
    public ResponseEntity<ApiResponse<JokeResponse>> createJoke(
            @Valid @RequestBody CreateJokeRequest request,
            @AuthenticationPrincipal TokenPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Joke posted successfully",
                        jokeService.createJoke(request, principal.userId())));
    }

    @Operation(summary = "Get a joke by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JokeResponse>> getJoke(@PathVariable UUID id) {
        return ResponseEntity.ok(
                ApiResponse.success("Joke fetched successfully", jokeService.getJokeById(id)));
    }

    @Operation(summary = "Search jokes with keyset pagination")
    @GetMapping
    public ResponseEntity<ApiResponse<JokePageResponse>> searchJokes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String categoryName,
            @RequestParam(required = false) Integer lastUpvotes,
            @RequestParam(required = false) UUID lastId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Jokes fetched successfully",
                        jokeService.searchJokes(query, categoryName, lastUpvotes, lastId, size)));
    }

    @Operation(summary = "Get jokes by category")
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<ApiResponse<Page<JokeResponse>>> getByCategory(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Jokes fetched successfully",
                        jokeService.getJokesByCategory(categoryName, page, size)));
    }

    @Operation(summary = "Get jokes by username")
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<Page<JokeResponse>>> getByUsername(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Jokes fetched successfully",
                        jokeService.getJokesByUsername(username, page, size)));
    }

    @Operation(summary = "Delete a joke", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJoke(
            @PathVariable UUID id,
            @AuthenticationPrincipal TokenPrincipal principal) {
        jokeService.deleteJoke(id, principal.userId());
        return ResponseEntity.ok(ApiResponse.success("Joke deleted successfully", null));
    }
}