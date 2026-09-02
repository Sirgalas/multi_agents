package ru.sergalas.orchestrator.service;

import ru.sergalas.orchestrator.dto.request.UserCreateRequest;
import ru.sergalas.orchestrator.entity.User;

import java.util.List;

public interface UserService {
    List<User> findAll();
    User findById(Long id);
    User findByUsername(String username);
    User createUser(UserCreateRequest request);
    void deleteUser(Long id);
}