package com.application.jokester.vote.controller;

import com.application.jokester.auth.security.UserPrincipal;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.vote.entity.VoteType;
import com.application.jokester.vote.service.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jokes")
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/{id}/vote")
    public ResponseEntity<JokeResponse> vote(
            @PathVariable UUID id,
            @RequestParam VoteType type,
            // ✅ Fixed: UserPrincipal not User
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(voteService.vote(id, type, userPrincipal.getUser()));
    }
}