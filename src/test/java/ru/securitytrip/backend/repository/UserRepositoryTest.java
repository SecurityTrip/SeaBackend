package ru.securitytrip.backend.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.securitytrip.backend.model.User;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() throws Exception {
        User user = new User();
        
        Field usernameField = User.class.getDeclaredField("username");
        usernameField.setAccessible(true);
        usernameField.set(user, "testuser");
        
        Field passwordField = User.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(user, "password");
        
        Field avatarIdField = User.class.getDeclaredField("avatarId");
        avatarIdField.setAccessible(true);
        avatarIdField.set(user, 1);
        
        userRepository.save(user);
        
        assertTrue(userRepository.findByUsername("testuser").isPresent());
        User foundUser = userRepository.findByUsername("testuser").get();
        assertEquals("testuser", usernameField.get(foundUser));
    }
} 