package ru.securitytrip.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import ru.securitytrip.backend.dto.*;
import ru.securitytrip.backend.service.GameService;

@Tag(name = "WebSocket Морской Бой (Multiplayer)", description = "WebSocket API для мультиплеерной игры в морской бой. Все методы работают только для multiplayer режима.\n\nКаналы:\n- /app/multiplayer.create (создать комнату)\n- /app/multiplayer.join (подключиться по коду)\n- /app/multiplayer.move (сделать ход)\n- /app/multiplayer.state (получить состояние)")
@Controller
public class MultiplayerGameWebSocketController {

    @Autowired
    private GameService gameService;

    // Создание игры, генерация кода
    @Operation(summary = "Создание мультиплеерной игры", description = "Метод для создания новой игры и генерации кода игры")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно создана"),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @MessageMapping("/multiplayer.create")
    @SendTo("/topic/multiplayer/code")
    public MultiplayerGameCodeResponse createMultiplayerGame(@Payload CreateMultiplayerGameRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = Math.abs((long) username.hashCode());
        String gameCode = gameService.createMultiplayerGame(userId, request.getShips());
        return new MultiplayerGameCodeResponse(gameCode);
    }

    // Подключение по коду
    @Operation(summary = "Подключение к мультиплеерной игре", description = "Метод для подключения игрока к существующей игре по коду")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное подключение к игре"),
            @ApiResponse(responseCode = "400", description = "Некорректный код игры или запрос"),
            @ApiResponse(responseCode = "404", description = "Игра не найдена"),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера")
    })
    @MessageMapping("/multiplayer.join")
    @SendTo("/topic/multiplayer/join")
    public GameDto joinMultiplayerGame(@Payload JoinMultiplayerGameRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = Math.abs((long) username.hashCode());
        return gameService.joinMultiplayerGame(request.getGameCode(), userId, request.getShips());
    }

    // Ходы и состояние реализуются аналогично: /app/multiplayer.move, /app/multiplayer.state
}
