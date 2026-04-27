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