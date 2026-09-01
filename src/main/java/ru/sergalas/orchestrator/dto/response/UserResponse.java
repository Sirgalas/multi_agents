package ru.sergalas.orchestrator.dto.response;

import ru.sergalas.orchestrator.entity.enums.Role;

public record UserResponse(
    Long id,
    String username,
    Role role
) {}