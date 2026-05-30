package com.application.jokester.config;

import java.util.UUID;

public record TokenPrincipal(UUID userId, String username) {}