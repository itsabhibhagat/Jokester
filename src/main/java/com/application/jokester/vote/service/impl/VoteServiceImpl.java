package com.application.jokester.vote.service.impl;

import com.application.jokester.auth.entity.User;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VoteServiceImpl implements VoteService {

    private final VoteRepository voteRepository;
    private final JokeRepository jokeRepository;

    @Override
    @CacheEvict(value = "joke", key = "#jokeId", beforeInvocation = true)
    public JokeResponse vote(UUID jokeId, VoteType voteType, User currentUser) {

        //Fetch joke with JOIN FETCH — loads user + category in one query
        Joke joke = jokeRepository.findByIdWithDetails(jokeId)
                .orElseThrow(() -> new RuntimeException("Joke not found: " + jokeId));

        Optional<Vote> existingVote =
                voteRepository.findByUserIdAndJokeId(currentUser.getId(), jokeId);

        if (existingVote.isPresent()) {
            Vote vote = existingVote.get();

            if (vote.getVoteType() == voteType) {
                //Same vote clicked again → TOGGLE OFF
                // Example: already upvoted → click upvote again → remove upvote
                log.info("Toggling off {} vote for joke {}", voteType, jokeId);
                undoVoteAtomic(jokeId, voteType);
                voteRepository.delete(vote);

            } else {
                //Different vote clicked → SWITCH
                // Example: upvoted → click downvote → upvotes--, downvotes++
                log.info("Switching vote from {} to {} for joke {}", vote.getVoteType(), voteType, jokeId);
                switchVoteAtomic(jokeId, vote.getVoteType(), voteType);
                vote.setVoteType(voteType);
                voteRepository.save(vote);
            }

        } else {
            // ➕ No existing vote → NEW VOTE
            log.info("New {} vote for joke {}", voteType, jokeId);
            applyVoteAtomic(jokeId, voteType);

            Vote newVote = Vote.builder()
                    .id(new VoteId(currentUser.getId(), jokeId))
                    .user(currentUser)
                    .joke(joke)  //reuse already fetched joke
                    .voteType(voteType)
                    .build();

            voteRepository.save(newVote);
        }

        //Fixed: use findByIdWithDetails — avoids lazy load of user/category
        Joke updatedJoke = jokeRepository.findByIdWithDetails(jokeId)
                .orElseThrow(() -> new RuntimeException("Joke not found"));
        return toResponse(updatedJoke);
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

//    @Override public void applyVote(Joke joke, VoteType type) {}
//    @Override public void undoVote(Joke joke, VoteType type) {}
}