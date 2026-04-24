package com.application.jokester.random.dto;

import lombok.*;

import java.io.Serializable;

// implements Serializable — required for Redis caching
// RandomJokeService uses @Cacheable("random-jokes") which stores this in Redis
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RandomJokeResponse implements Serializable {
    private String joke;
    private String provider;
}