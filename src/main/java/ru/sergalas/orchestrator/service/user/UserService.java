package ru.sergalas.orchestrator.service.user;

import ru.sergalas.orchestrator.dto.request.CreateUserRequest;
import ru.sergalas.orchestrator.entity.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User createUser(CreateUserRequest request);
    User getCurrentUser();
    User getUserByUsername(String username);
    void deleteUser(Long id);
}