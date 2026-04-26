package com.application.jokester.random.controller;

import com.application.jokester.common.ApiResponse;
import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.service.RandomJokeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jokes/random")
@RequiredArgsConstructor
@Tag(name = "Random Jokes", description = "Endpoints for fetching jokes from third-party providers")
public class RandomJokeController {

    private final RandomJokeService randomJokeService;

    // Fetches a joke from a randomly selected third-party provider.
    // The first call hits the external API which takes about 500ms.
    // Subsequent calls within 1 hour are served from Redis cache in under 5ms.
    // No authentication required — this is a public endpoint.
    @Operation(
            summary = "Get a random joke from any provider",
            description = "Fetches a joke from a randomly selected provider. First call hits external API (~500ms). Cached in Redis for 1 hour after that (~5ms)."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<RandomJokeResponse>> randomJoke() {
        return ResponseEntity.ok(
                ApiResponse.success("Random joke fetched successfully",
                        randomJokeService.fromRandomProvider()));
    }

    // Fetches a joke from a specific named provider.
    // Available providers are jokeApi and ChuckNorris.
    // The provider name must match exactly what is returned by the providers endpoint.
    // Results are cached per provider name for 1 hour.
    @Operation(
            summary = "Get a joke from a specific provider",
            description = "Fetches a joke from a named provider. Use the providers endpoint to see available provider names. Results are cached per provider for 1 hour."
    )
    @GetMapping("/{provider}")
    public ResponseEntity<ApiResponse<RandomJokeResponse>> jokeFromProvider(
            @Parameter(description = "Provider name, e.g. jokeApi or ChuckNorris") @PathVariable String provider) {

        return ResponseEntity.ok(
                ApiResponse.success("Joke from " + provider + " fetched successfully",
                        randomJokeService.fromProvider(provider)));
    }

    // Lists all currently available third-party joke providers.
    // Use these names when calling the specific provider endpoint.
    // New providers can be added to the system by creating a new class
    // that implements the JokeProvider interface — no other changes needed.
    @Operation(
            summary = "List available joke providers",
            description = "Returns the names of all third-party joke providers currently available in the system."
    )
    @GetMapping("/providers")
    public ResponseEntity<ApiResponse<List<String>>> availableProviders() {
        return ResponseEntity.ok(
                ApiResponse.success("Providers fetched successfully",
                        randomJokeService.getAvailableProviders()));
    }
}