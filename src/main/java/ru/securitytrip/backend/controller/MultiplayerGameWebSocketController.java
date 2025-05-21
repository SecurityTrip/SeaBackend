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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import ru.securitytrip.backend.dto.*;
import ru.securitytrip.backend.service.GameService;
import ru.securitytrip.backend.model.GameMode;

@Tag(name = "WebSocket Морской Бой (Multiplayer)", description = "Production-ready WebSocket API для мультиплеерной игры в морской бой.\n\nКаналы:\n- /app/multiplayer.create (создать комнату)\n- /app/multiplayer.join (подключиться по коду)\n- /app/multiplayer.move (сделать ход)\n- /app/multiplayer.state (получить состояние)\n\nВсе методы работают только для multiplayer режима. Примеры сообщений и ответы приведены в описаниях методов.")
@RestController
@CrossOrigin(origins = "http://localhost")
public class MultiplayerGameWebSocketController {

    @Autowired
    private GameService gameService;

    // Создание игры, генерация кода
    @Operation(summary = "Создание мультиплеерной игры", description = "Создаёт новую комнату для мультиплеерной игры.\n\n**Отправить:** CreateMultiplayerGameRequest на /app/multiplayer.create\n**Получить:** MultiplayerGameCodeResponse по /topic/multiplayer/code.\n\nПример запроса:\n```json\n{\n  \"ships\": [\n    {\"size\": 4, \"x\": 0, \"y\": 0, \"horizontal\": true}\n  ]\n}\n```\n\nПример ответа:\n```json\n{\n  \"gameCode\": \"ABC123\"\n}\n```\n")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно создана", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MultiplayerGameCodeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера", content = @io.swagger.v3.oas.annotations.media.Content)
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
    @Operation(summary = "Подключение к мультиплеерной игре", description = "Подключает второго игрока к существующей комнате по коду.\n\n**Отправить:** JoinMultiplayerGameRequest на /app/multiplayer.join\n**Получить:** GameDto по /topic/multiplayer/join.\n\nПример запроса:\n```json\n{\n  \"gameCode\": \"ABC123\",\n  \"ships\": [\n    {\"size\": 4, \"x\": 0, \"y\": 0, \"horizontal\": true}\n  ]\n}\n```\n\nПример ответа GameDto см. в /topic/multiplayer/state.\n\nОшибки:\n- 400: Некорректный код или запрос\n- 404: Игра не найдена\n")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное подключение к игре", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GameDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный код игры или запрос", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @MessageMapping("/multiplayer.join")
    @SendTo("/topic/multiplayer/join")
    public GameDto joinMultiplayerGame(@Payload JoinMultiplayerGameRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = Math.abs((long) username.hashCode());
        return gameService.joinMultiplayerGame(request.getGameCode(), userId, request.getShips());
    }

    // Сделать ход в мультиплеерной игре
    @Operation(summary = "Сделать ход в мультиплеерной игре", description = "Выполняет ход игрока в мультиплеерной игре.\n\n**Отправить:** MoveRequest на /app/multiplayer.move\n**Получить:** GameDto по /topic/multiplayer/move.\n\nПример запроса:\n```json\n{\n  \"gameCode\": \"ABC123\",\n  \"x\": 5,\n  \"y\": 3\n}\n```\n\nПример ответа:\n```json\n{\n  \"mode\": \"multiplayer\",\n  \"gameState\": \"IN_PROGRESS\",\n  \"playerTurn\": true,\n  \"playerBoard\": { ... },\n  \"computerBoard\": { ... }\n}\n```\n\nОшибки:\n- 400: Некорректные параметры (например, не ваш ход, неверные координаты)\n- 404: Игра не найдена\n")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ход успешно выполнен", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GameDto.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @MessageMapping("/multiplayer.move")
    @SendTo("/topic/multiplayer/move")
    public GameDto makeMove(@Payload MoveRequest moveRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = Math.abs((long) username.hashCode());
        String gameCode = moveRequest.getGameCode();
        return gameService.makeMultiplayerMove(gameCode, userId, moveRequest);
    }

    // Получить состояние мультиплеерной игры
    @Operation(summary = "Получить состояние мультиплеерной игры", description = "Получить текущее состояние мультиплеерной игры.\n\n**Отправить:** gameCode (String) на /app/multiplayer.state\n**Получить:** GameDto по /topic/multiplayer/state.\n\nПример запроса:\n```json\n\"ABC123\"\n```\n\nПример ответа:\n```json\n{\n  \"mode\": \"multiplayer\",\n  \"gameState\": \"IN_PROGRESS\",\n  \"playerTurn\": true,\n  \"playerBoard\": { ... },\n  \"computerBoard\": { ... }\n}\n```\n\nОшибки:\n- 404: Игра не найдена\n")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Данные игры успешно получены", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GameDto.class))),
        @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @MessageMapping("/multiplayer.state")
    @SendTo("/topic/multiplayer/state")
    public GameDto getGameState(@Payload String gameCode) {
        GameDto gameState = gameService.getMultiplayerGameState(gameCode);
        // Убедимся, что режим игры установлен как multiplayer
        gameState.setMode(GameMode.multiplayer);
        return gameState;
    }
}
