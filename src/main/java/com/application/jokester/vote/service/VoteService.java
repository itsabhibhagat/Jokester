package com.application.jokester.vote.service;

import com.application.jokester.auth.entity.User;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.vote.entity.VoteType;

import java.util.UUID;

public interface VoteService {

    public JokeResponse vote(UUID jokeId, VoteType voteType, User currentUser);

//    public void applyVote(Joke joke, VoteType type);
//
//    public void undoVote(Joke joke, VoteType type);
}
