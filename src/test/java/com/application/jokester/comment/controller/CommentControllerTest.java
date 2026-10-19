package com.application.jokester.comment.controller;

import com.application.jokester.comment.dto.CommentResponse;
import com.application.jokester.comment.dto.CreateCommentRequest;
import com.application.jokester.comment.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.application.jokester.config.JwtService;
import com.application.jokester.config.UserDetailsServiceImpl;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import com.application.jokester.config.TokenPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CommentService commentService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private LettuceBasedProxyManager<String> proxyManager;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        TokenPrincipal mockPrincipal = new TokenPrincipal(testUserId, "testuser");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockPrincipal, null, List.of()));
    }

    @Test
    void should_Return201_When_AddComment() throws Exception {
        UUID jokeId = UUID.randomUUID();
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent("Hello World");

        CommentResponse res = CommentResponse.builder().id(UUID.randomUUID()).content("Hello World").build();
        when(commentService.addComment(eq(jokeId), any(), any())).thenReturn(res);

        mockMvc.perform(post("/api/jokes/{jokeId}/comments", jokeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void should_Return200_When_GetComments() throws Exception {
        UUID jokeId = UUID.randomUUID();
        CommentResponse res = CommentResponse.builder().id(UUID.randomUUID()).build();
        when(commentService.getCommentsByJoke(jokeId)).thenReturn(List.of(res));

        mockMvc.perform(get("/api/jokes/{jokeId}/comments", jokeId))
                .andExpect(status().isOk());
    }

    @Test
    void should_Return200_When_LikeComment() throws Exception {
        UUID commentId = UUID.randomUUID();
        CommentResponse res = CommentResponse.builder().id(commentId).build();
        when(commentService.likeComment(eq(commentId), any())).thenReturn(res);

        mockMvc.perform(post("/api/comments/{commentId}/like", commentId))
                .andExpect(status().isOk());
    }
}
