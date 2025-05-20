package ru.securitytrip.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.securitytrip.backend.dto.UpdateUserRequest;
import ru.securitytrip.backend.repository.UserRepository;
import ru.securitytrip.backend.model.User;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId).orElseThrow();
        if (request != null) {
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                user.setUsername(request.getUsername());
            }
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                user.setPassword(passwordEncoder.encode(request.getPassword()));
            }
            if (request.getAvatarId() != null) {
                user.setAvatarId(request.getAvatarId());
            }
            userRepository.save(user);
        }
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }
}
