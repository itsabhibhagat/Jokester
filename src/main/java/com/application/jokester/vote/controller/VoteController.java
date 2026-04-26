package com.application.jokester.vote.controller;

import com.application.jokester.auth.security.UserPrincipal;
import com.application.jokester.common.ApiResponse;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.vote.entity.VoteType;
import com.application.jokester.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jokes")
@Tag(name = "Votes", description = "Endpoints for upvoting and downvoting jokes")
public class VoteController {

    private final VoteService voteService;

    // Allows a logged-in user to cast a vote on a joke.
    // Voting has three behaviors depending on the user's current vote status:
    // 1. If the user has not voted yet, the vote is added.
    // 2. If the user votes the same way again, the vote is removed (toggle off).
    // 3. If the user switches from upvote to downvote or vice versa, the old vote is
    //    removed and the new one is applied — ensuring only one vote type exists per user.
    // This enforces the rule that a user can either upvote OR downvote, never both.
    @Operation(
            summary = "Vote on a joke",
            description = "Cast a vote on a joke. Voting the same type again removes the vote. Switching vote type updates it. A user can only have one active vote per joke. Requires authentication.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<JokeResponse>> vote(
            @Parameter(description = "The unique ID of the joke to vote on") @PathVariable UUID id,
            @Parameter(description = "Vote type: UP or DOWN") @RequestParam VoteType type,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        JokeResponse updatedJoke = voteService.vote(id, type, userPrincipal.getUser());
        return ResponseEntity.ok(
                ApiResponse.success("Vote recorded successfully", updatedJoke));
    }
}