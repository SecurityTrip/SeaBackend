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
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import ru.securitytrip.backend.dto.*;
import ru.securitytrip.backend.service.GameService;
import ru.securitytrip.backend.model.GameMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Tag(name = "WebSocket Морской Бой (Multiplayer)", description = "Production-ready WebSocket API для мультиплеерной игры в морской бой.\n\nКаналы:\n- /app/multiplayer.create (создать комнату)\n- /app/multiplayer.join (подключиться по коду)\n- /app/multiplayer.move (сделать ход)\n- /app/multiplayer.state (получить состояние)\n\nВсе методы работают только для multiplayer режима. Примеры сообщений и ответы приведены в описаниях методов.")
@RestController
@CrossOrigin(origins = "http://localhost")
public class MultiplayerGameWebSocketController {

    private static final Logger logger = LoggerFactory.getLogger(MultiplayerGameWebSocketController.class);

    @Autowired
    private GameService gameService;

    @Operation(summary = "Размещение кораблей хоста", description = "Хост отправляет свою расстановку кораблей после создания комнаты.\n\n**Отправить:** PlaceHostShipsRequest на /app/multiplayer.place\n**Получить:** GameDto по /topic/multiplayer/place.\n\nПример запроса:\n```json\n{\n  \"gameCode\": \"ABC123\",\n  \"ships\": [ ... ],\n  \"userId\": 123456\n}\n```\n\nОшибки:\n- 400: Некорректный код или запрос\n- 404: Игра не найдена\n")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Корабли успешно размещены", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GameDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный код игры или запрос", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @MessageMapping("/multiplayer.place")
    @SendTo("/topic/multiplayer/place")
    public GameDto placeHostShips(@Payload PlaceHostShipsRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = request.getUserId();
        String username = "anonymous";
        Object userObj = headerAccessor.getUser();
        if (userObj != null) {
            String name = ((java.security.Principal)userObj).getName();
            if (name != null) {
                username = name;
            }
        }
        logger.info("[MULTIPLAYER] Хост {} (userId={}) размещает корабли в комнате {}: {}", username, userId, request.getGameCode(), request.getShips());
        GameDto dto = gameService.placeHostShips(request.getGameCode(), userId, request.getShips());
        logger.info("[MULTIPLAYER] Корабли хоста размещены для userId={} в комнате {}", userId, request.getGameCode());
        return dto;
    }

    // Создание игры, генерация кода
    @Operation(summary = "Создание мультиплеерной игры", description = "Создаёт новую комнату для мультиплеерной игры.\n\n**Отправить:** CreateMultiplayerGameRequest на /app/multiplayer.create\n**Получить:** MultiplayerGameCodeResponse по /topic/multiplayer/code.\n\nПример запроса:\n```json\n{\n  \"ships\": [\n    {\"size\": 4, \"x\": 0, \"y\": 0, \"horizontal\": true}\n  ],\n  \"userId\": 123456\n}\n```\n\nПример ответа:\n```json\n{\n  \"gameCode\": \"ABC123\"\n}\n```")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно создана", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MultiplayerGameCodeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректный запрос", content = @io.swagger.v3.oas.annotations.media.Content),
            @ApiResponse(responseCode = "500", description = "Ошибка сервера", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @MessageMapping("/multiplayer.create")
    @SendTo("/topic/multiplayer/code")
    public MultiplayerGameCodeResponse createMultiplayerGame(@Payload CreateMultiplayerGameRequest request, SimpMessageHeaderAccessor headerAccessor) {
        Long userId = request.getUserId();
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        logger.info("[MULTIPLAYER] Запрос на создание комнаты от пользователя: {} (userId={}), ships={}", username, userId, request.getShips());
        String gameCode = gameService.createMultiplayerGame(userId, request.getShips());
        logger.info("[MULTIPLAYER] Комната создана: {} для userId={}", gameCode, userId);
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
        Long userId = request.getUserId();
        String username = "anonymous";
        Object userObj = headerAccessor.getUser();
        if (userObj != null) {
            String name = ((java.security.Principal)userObj).getName();
            if (name != null) {
                username = name;
            }
        }
        logger.info("[MULTIPLAYER] Пользователь {} (userId={}) подключается к комнате {} с кораблями {}", username, userId, request.getGameCode(), request.getShips());
        GameDto dto = gameService.joinMultiplayerGame(request.getGameCode(), userId, request.getShips());
        logger.info("[MULTIPLAYER] Пользователь {} (userId={}) успешно подключён к комнате {}", username, userId, request.getGameCode());
        return dto;
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
        Long userId = moveRequest.getUserId();
        String username = "anonymous";
        Object userObj = headerAccessor.getUser();
        if (userObj != null) {
            String name = ((java.security.Principal)userObj).getName();
            if (name != null) {
                username = name;
            }
        }
        String gameCode = moveRequest.getGameCode();
        logger.info("[MULTIPLAYER] Пользователь {} (userId={}) делает ход в комнате {}: x={}, y={}", username, userId, gameCode, moveRequest.getX(), moveRequest.getY());
        GameDto dto = gameService.makeMultiplayerMove(gameCode, userId, moveRequest);
        logger.info("[MULTIPLAYER] Ход пользователя {} (userId={}) в комнате {} обработан", username, userId, gameCode);
        return dto;
    }

    // Получить состояние мультиплеерной игры
    @Operation(summary = "Получить состояние мультиплеерной игры", description = "Получить текущее состояние мультиплеерной игры.\n\n**Отправить:** gameCode (String) на /app/multiplayer.state\n**Получить:** GameDto по /topic/multiplayer/state.\n\nПример запроса:\n```json\n\"ABC123\"\n```\n\nПример ответа:\n```json\n{\n  \"mode\": \"multiplayer\",\n  \"gameState\": \"IN_PROGRESS\",\n  \"playerTurn\": true,\n  \"playerBoard\": { ... },\n  \"computerBoard\": { ... }\n}\n```\n\nОшибки:\n- 404: Игра не найдена\n")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Данные игры успешно получены", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = GameDto.class))),
        @ApiResponse(responseCode = "404", description = "Игра не найдена", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    @MessageMapping("/multiplayer.state")
    @SendToUser("/topic/multiplayer/state")
    public GameDto getGameState(@Payload MultiplayerGameStateRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String gameCode = request.getGameCode();
        Long userId = null;
        try {
            userId = Long.parseLong(request.getUserId());
        } catch (Exception e) {
            logger.warn("[MULTIPLAYER] Некорректный userId в запросе состояния: {}", request.getUserId());
        }
        if (userId == null) {
            // Fallback: если не удалось — используем старую логику (НЕ рекомендуется)
            String username = "anonymous";
            Object userObj = headerAccessor.getUser();
            if (userObj != null) {
                String name = ((java.security.Principal)userObj).getName();
                if (name != null) {
                    username = name;
                }
            }
            userId = Math.abs((long) username.hashCode());
        }
        String cleanCode = gameCode.trim().replaceAll("^[\"']|[\"']$", "");
        logger.info("[MULTIPLAYER] Запрос состояния комнаты {} (очищено: {}) для userId={}", gameCode, cleanCode, userId);
        GameDto gameState = gameService.getMultiplayerGameState(cleanCode, userId);
        gameState.setMode(GameMode.multiplayer);
        logger.info("[MULTIPLAYER] Состояние комнаты {} отправлено для userId={}", cleanCode, userId);
        return gameState;
    }
}
