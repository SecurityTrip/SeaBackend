package ru.securitytrip.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GameServiceTest {
    @Autowired
    private GameService gameService;

    @Test
    void testGeneratePlayerShipsNotNull() {
        assertNotNull(gameService.generatePlayerShips("RANDOM"));
    }
} 