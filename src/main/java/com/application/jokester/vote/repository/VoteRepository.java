package com.application.jokester.vote.repository;

import com.application.jokester.vote.entity.Vote;
import com.application.jokester.vote.entity.VoteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, VoteId> {

    @Query("""
        SELECT v FROM Vote v
        WHERE v.id.userId = :userId
        AND v.id.jokeId = :jokeId
    """)
    Optional<Vote> findByUserIdAndJokeId(
            @Param("userId") UUID userId,
            @Param("jokeId") UUID jokeId
    );
}