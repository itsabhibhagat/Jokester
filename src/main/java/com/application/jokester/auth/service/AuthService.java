package com.application.jokester.auth.service;

import com.application.jokester.auth.dto.AuthResponse;
import com.application.jokester.auth.dto.LoginRequest;
import com.application.jokester.auth.dto.RegisterRequest;
import com.application.jokester.auth.entity.User;
import com.application.jokester.auth.repository.UserRepository;
import com.application.jokester.config.JwtService;
import com.application.jokester.joke.repository.JokeRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

public interface AuthService {

    public AuthResponse register(RegisterRequest request);

    public AuthResponse login(LoginRequest request);
}