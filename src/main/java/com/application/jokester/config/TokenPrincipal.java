package com.application.jokester.config;

import java.util.UUID;

// Lightweight principal built from JWT claims without any database query.
// Holds only what controllers need — userId and username.
public record TokenPrincipal(UUID userId, String username) {}