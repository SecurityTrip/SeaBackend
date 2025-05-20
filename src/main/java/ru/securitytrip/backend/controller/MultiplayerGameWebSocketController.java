package ru.securitytrip.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import ru.securitytrip.backend.dto.*;
import ru.securitytrip.backend.service.GameService;

@Controller
public class MultiplayerGameWebSocketController {

    @Autowired
    private GameService gameService;

    // Создание игры, генерация кода
    @MessageMapping("/multiplayer.create")
    @SendTo("/topic/multiplayer/code")
    public MultiplayerGameCodeResponse createMultiplayerGame(@Payload CreateMultiplayerGameRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = Math.abs((long) username.hashCode());
        String gameCode = gameService.createMultiplayerGame(userId, request.getShips());
        return new MultiplayerGameCodeResponse(gameCode);
    }

    // Подключение по коду
    @MessageMapping("/multiplayer.join")
    @SendTo("/topic/multiplayer/join")
    public GameDto joinMultiplayerGame(@Payload JoinMultiplayerGameRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = Math.abs((long) username.hashCode());
        return gameService.joinMultiplayerGame(request.getGameCode(), userId, request.getShips());
    }

    // Ходы и состояние реализуются аналогично: /app/multiplayer.move, /app/multiplayer.state
}
