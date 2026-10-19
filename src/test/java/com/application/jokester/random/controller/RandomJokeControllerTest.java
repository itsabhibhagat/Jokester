package com.application.jokester.random.controller;

import com.application.jokester.config.JwtService;
import com.application.jokester.config.UserDetailsServiceImpl;
import com.application.jokester.random.dto.RandomJokeResponse;
import com.application.jokester.random.service.RandomJokeService;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RandomJokeController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RandomJokeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RandomJokeService randomJokeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private LettuceBasedProxyManager<String> proxyManager;

    @Test
    void should_Return200_When_GetRandomJoke() throws Exception {
        RandomJokeResponse response = RandomJokeResponse.builder()
                .joke("Why did the chicken cross the road? To get to the other side.")
                .provider("jokeApi")
                .build();

        when(randomJokeService.fromRandomProvider()).thenReturn(response);

        mockMvc.perform(get("/api/jokes/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joke").value("Why did the chicken cross the road? To get to the other side."))
                .andExpect(jsonPath("$.data.provider").value("jokeApi"))
                .andExpect(jsonPath("$.message").value("Random joke fetched successfully"));
    }

    @Test
    void should_Return200_When_GetFromProvider() throws Exception {
        RandomJokeResponse response = RandomJokeResponse.builder()
                .joke("Chuck Norris can divide by zero.")
                .provider("ChuckNorris")
                .build();

        when(randomJokeService.fromProvider("ChuckNorris")).thenReturn(response);

        mockMvc.perform(get("/api/jokes/random/ChuckNorris"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.joke").value("Chuck Norris can divide by zero."))
                .andExpect(jsonPath("$.data.provider").value("ChuckNorris"))
                .andExpect(jsonPath("$.message").value("Joke from ChuckNorris fetched successfully"));
    }

    @Test
    void should_Return200_When_ListProviders() throws Exception {
        List<String> providers = List.of("jokeApi", "ChuckNorris");

        when(randomJokeService.getAvailableProviders()).thenReturn(providers);

        mockMvc.perform(get("/api/jokes/random/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("jokeApi"))
                .andExpect(jsonPath("$.data[1]").value("ChuckNorris"))
                .andExpect(jsonPath("$.message").value("Providers fetched successfully"));
    }
}
