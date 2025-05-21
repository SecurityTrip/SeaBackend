package ru.securitytrip.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserDetailsServiceImplTest {
    @Autowired
    private UserDetailsServiceImpl userDetailsServiceImpl;

    @Test
    void testLoadUserByUsernameThrows() {
        assertThrows(Exception.class, () -> userDetailsServiceImpl.loadUserByUsername("nonexistent"));
    }
} 