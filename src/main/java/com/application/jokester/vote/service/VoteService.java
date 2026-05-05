package com.application.jokester.vote.service;

import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.vote.entity.VoteType;

import java.util.UUID;

public interface VoteService {
    JokeResponse vote(UUID jokeId, VoteType voteType, UUID userId);
}