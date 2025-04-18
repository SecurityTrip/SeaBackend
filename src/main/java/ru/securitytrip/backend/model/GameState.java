package ru.securitytrip.backend.model;

public enum GameState {
    WAITING,     // Ожидание начала
    IN_PROGRESS, // Игра идет
    PLAYER_WON,  // Победа игрока
    COMPUTER_WON // Победа компьютера
} 