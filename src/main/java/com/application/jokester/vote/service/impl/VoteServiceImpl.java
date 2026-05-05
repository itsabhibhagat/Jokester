package com.application.jokester.vote.service.impl;

import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.exception.ResourceNotFoundException;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.entity.Joke;
import com.application.jokester.joke.repository.JokeRepository;
import com.application.jokester.vote.entity.Vote;
import com.application.jokester.vote.entity.VoteId;
import com.application.jokester.vote.entity.VoteType;
import com.application.jokester.vote.repository.VoteRepository;
import com.application.jokester.vote.service.VoteService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final JokeRepository jokeRepository;
    private final UserRepository userRepository;

    @Override
    @CacheEvict(value = "joke", key = "#jokeId", beforeInvocation = true)
    public JokeResponse vote(UUID jokeId, VoteType voteType, UUID userId) {
        Joke joke = jokeRepository.findByIdWithDetails(jokeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joke not found: " + jokeId));

        voteRepository.findByUserIdAndJokeId(userId, jokeId)
                .ifPresentOrElse(
                        existing -> {
                            if (existing.getVoteType() == voteType) {
                                undoVoteAtomic(jokeId, voteType);
                                voteRepository.delete(existing);
                            } else {
                                switchVoteAtomic(jokeId, existing.getVoteType(), voteType);
                                existing.setVoteType(voteType);
                                voteRepository.save(existing);
                            }
                        },
                        () -> {
                            applyVoteAtomic(jokeId, voteType);
                            // getReferenceById avoids a full SELECT — loads a proxy reference only
                            User userRef = userRepository.getReferenceById(userId);
                            voteRepository.save(Vote.builder()
                                    .id(new VoteId(userId, jokeId))
                                    .user(userRef)
                                    .joke(joke)
                                    .voteType(voteType)
                                    .build());
                        }
                );

        return jokeRepository.findByIdWithDetails(jokeId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Joke not found: " + jokeId));
    }

    private void applyVoteAtomic(UUID jokeId, VoteType type) {
        if (type == VoteType.UP) jokeRepository.incrementUpvotes(jokeId);
        else jokeRepository.incrementDownvotes(jokeId);
    }

    private void undoVoteAtomic(UUID jokeId, VoteType type) {
        if (type == VoteType.UP) jokeRepository.decrementUpvotes(jokeId);
        else jokeRepository.decrementDownvotes(jokeId);
    }

    private void switchVoteAtomic(UUID jokeId, VoteType from, VoteType to) {
        undoVoteAtomic(jokeId, from);
        applyVoteAtomic(jokeId, to);
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
                .createdAt(joke.getCreated_at())
                .build();
    }
}