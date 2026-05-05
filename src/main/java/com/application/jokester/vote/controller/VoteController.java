package com.application.jokester.vote.controller;

import com.application.jokester.common.ApiResponse;
import com.application.jokester.config.TokenPrincipal;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.vote.entity.VoteType;
import com.application.jokester.vote.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Votes", description = "Endpoints for voting on jokes")
public class VoteController {

    private final VoteService voteService;

    @Operation(
            summary = "Vote on a joke",
            description = "Cast a vote (UP or DOWN) on a joke. Voting the same type removes it. Switching type updates it.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<JokeResponse>> vote(
            @PathVariable UUID id,
            @RequestParam VoteType type,
            @AuthenticationPrincipal TokenPrincipal principal) {
        return ResponseEntity.ok(
                ApiResponse.success("Vote recorded successfully",
                        voteService.vote(id, type, principal.userId())));
    }
}