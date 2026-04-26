package com.application.jokester.joke.controller;

import com.application.jokester.auth.security.UserPrincipal;
import com.application.jokester.common.ApiResponse;
import com.application.jokester.joke.dto.CreateJokeRequest;
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

    // Allows a logged-in user to post a new joke.
    // The user provides a title, content, and a category name.
    // If the category does not exist yet, it is created automatically.
    // Authentication is required — only registered users can post jokes.
    @Operation(
            summary = "Post a new joke",
            description = "Creates a new joke. If the category name does not exist, a new category is created automatically. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/post")
    public ResponseEntity<ApiResponse<JokeResponse>> createJoke(
            @Valid @RequestBody CreateJokeRequest createJokeRequest,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JokeResponse joke = jokeService.createJoke(createJokeRequest, userPrincipal.getUser());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Joke posted successfully", joke));
    }

    // Returns a single joke by its unique ID.
    // This endpoint is public — no login required.
    // Results are served from Redis cache after the first request, making it very fast.
    @Operation(
            summary = "Get a joke by ID",
            description = "Fetches a single joke using its unique ID. No authentication required. Results are cached for 10 minutes."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JokeResponse>> getJoke(
            @Parameter(description = "The unique ID of the joke") @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success("Joke fetched successfully", jokeService.getJokeById(id)));
    }

    // Searches jokes using an optional text keyword and optional category name.
    // Both filters are optional — if nothing is provided, all jokes are returned sorted by upvotes.
    // This endpoint is public — no login required.
    // Results are paginated to avoid returning millions of records at once.
    @Operation(
            summary = "Search jokes",
            description = "Search jokes by keyword and category name. Both parameters are optional. Results are sorted by upvotes and paginated."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<Page<JokeResponse>>> searchJokes(
            @Parameter(description = "Keyword to search in joke title and content") @RequestParam(required = false) String query,
            @Parameter(description = "Category name to filter by, e.g. Animal Jokes") @RequestParam(required = false) String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<JokeResponse> results = jokeService.searchJokes(query, categoryName, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Jokes fetched successfully", results));
    }

    // Returns all jokes that belong to a specific category.
    // The category name is case-insensitive — Animal Jokes and animal jokes are treated the same.
    // This endpoint is public — no login required.
    @Operation(
            summary = "Get jokes by category",
            description = "Fetches all jokes in a specific category. Category name is case-insensitive. Results are sorted by upvotes."
    )
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<ApiResponse<Page<JokeResponse>>> getByCategory(
            @Parameter(description = "Category name, e.g. Animal Jokes") @PathVariable String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<JokeResponse> results = jokeService.getJokesByCategory(categoryName, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Jokes for category '" + categoryName + "' fetched successfully", results));
    }

    // Returns all jokes posted by a specific username.
    // This lets anyone view a user's posted joke history.
    // This endpoint is public — no login required.
    @Operation(
            summary = "Get jokes by username",
            description = "Fetches all jokes posted by a specific user. Results are sorted by most recent first."
    )
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<Page<JokeResponse>>> getByUsername(
            @Parameter(description = "The username to search jokes for") @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<JokeResponse> results = jokeService.getJokesByUsername(username, page, size);
        return ResponseEntity.ok(
                ApiResponse.success("Jokes by user '" + username + "' fetched successfully", results));
    }

    // Permanently deletes a joke from the database.
    // Only the user who posted the joke can delete it.
    // The joke is also removed from Redis cache immediately.
    // Authentication is required.
    @Operation(
            summary = "Delete a joke",
            description = "Permanently deletes a joke. Only the user who posted it can delete it. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJoke(
            @Parameter(description = "The unique ID of the joke to delete") @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        jokeService.deleteJoke(id, userPrincipal.getUser());
        return ResponseEntity.ok(
                ApiResponse.success("Joke deleted successfully", null));
    }
}