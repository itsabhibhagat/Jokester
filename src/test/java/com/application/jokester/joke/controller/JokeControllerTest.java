package com.application.jokester.joke.controller;

import com.application.jokester.config.TokenPrincipal;
import com.application.jokester.joke.dto.CreateJokeRequest;
import com.application.jokester.joke.dto.JokePageResponse;
import com.application.jokester.joke.dto.JokeResponse;
import com.application.jokester.joke.service.JokeService;
import com.application.jokester.config.JwtService;
import com.application.jokester.config.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JokeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class JokeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JokeService jokeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private LettuceBasedProxyManager<String> proxyManager;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        TokenPrincipal mockPrincipal = new TokenPrincipal(testUserId, "testuser");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(mockPrincipal, null, List.of()));
    }

    @Test
    void should_Return201_When_PostJoke() throws Exception {
        CreateJokeRequest request = new CreateJokeRequest();
        request.setTitle("Joke Title");
        request.setContent("Joke Content");
        request.setCategoryName("Pun");

        JokeResponse response = new JokeResponse();
        response.setTitle("Joke Title");

        when(jokeService.createJoke(any(CreateJokeRequest.class), eq(testUserId))).thenReturn(response);

        mockMvc.perform(post("/api/jokes/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Joke Title"));
    }

    @Test
    void should_Return200_When_GetJokeById() throws Exception {
        UUID jokeId = UUID.randomUUID();
        JokeResponse response = new JokeResponse();
        response.setId(jokeId);

        when(jokeService.getJokeById(jokeId)).thenReturn(response);

        mockMvc.perform(get("/api/jokes/{id}", jokeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(jokeId.toString()));
    }

    @Test
    void should_Return200_When_SearchJokes() throws Exception {
        JokePageResponse pageResponse = JokePageResponse.builder()
                .jokes(List.of(new JokeResponse()))
                .size(1)
                .hasMore(false)
                .build();

        when(jokeService.searchJokes(any(), any(), any(), any(), eq(10))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/jokes")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.size").value(1));
    }

    @Test
    void should_Return200_When_GetByCategory() throws Exception {
        Page<JokeResponse> page = new PageImpl<>(List.of(new JokeResponse()));

        when(jokeService.getJokesByCategory(eq("Pun"), eq(0), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/jokes/category/{name}", "Pun")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
