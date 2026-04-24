package com.application.jokester.random.controller;

import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.service.RandomJokeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/jokes/random")
@RequiredArgsConstructor
public class RandomJokeController {

    private final RandomJokeService randomJokeService;

    @GetMapping //getting jokes from random provider
    public ResponseEntity<RandomJokeResponse> RandomJoke() {
        return ResponseEntity.ok(randomJokeService.fromRandomProvider());
    }

    @GetMapping("/{provider}")
    public ResponseEntity<RandomJokeResponse> jokeFromProvider(@PathVariable String provider) {
        return ResponseEntity.ok(randomJokeService.fromProvider(provider));
    }

    @GetMapping("/providers")
    public ResponseEntity<List<String>> allAvailableProviders(){
        return ResponseEntity.ok(randomJokeService.getAvailableProviders());
    }
}
