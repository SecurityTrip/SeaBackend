package ru.securitytrip.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import ru.securitytrip.backend.dto.CreateSinglePlayerGameRequest;
import ru.securitytrip.backend.dto.GameDto;
import ru.securitytrip.backend.dto.MoveRequest;
import ru.securitytrip.backend.dto.MoveResponse;
import ru.securitytrip.backend.model.User;
import ru.securitytrip.backend.service.GameService;
import ru.securitytrip.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.security.Principal;

@Tag(name = "WebSocket Морской Бой (Singleplayer)", description = "Production-ready WebSocket API для одиночной игры в морской бой.\n\nКаналы:\n- /app/singleplayer.create (создать игру)\n- /app/singleplayer.move (сделать ход)\n- /app/singleplayer.state (получить состояние)\n\nВсе методы работают только для singleplayer режима. Примеры сообщений доступны в описаниях методов.")
@Controller
@CrossOrigin(origins = "http://localhost")
public class GameWebSocketController {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserService userService;

    @Operation(summary = "Создать одиночную игру", description = "Создаёт новую singleplayer игру с расстановкой кораблей игрока.\n\n**Отправить:** CreateSinglePlayerGameRequest на /app/singleplayer.create\n**Получить:** GameDto по /topic/singleplayer/game.\n\nПример запроса:\n```json\n{\n  \"ships\": [\n    {\"size\": 4, \"x\": 0, \"y\": 0, \"horizontal\": true}\n  ],\n  \"difficultyLevel\": \"EASY\"\n}\n```\n\nПример ответа GameDto см. в /topic/singleplayer/state.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Игра успешно создана", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDto.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные параметры", content = @Content),
        @ApiResponse(responseCode = "401", description = "Не авторизован", content = @Content)
    })
    @MessageMapping("/singleplayer.create")
    @SendTo("/topic/singleplayer/game")
    public GameDto createSinglePlayerGame(
            @Parameter(description = "Данные расстановки кораблей игрока", required = true)
            @Payload CreateSinglePlayerGameRequest request,
            Principal principal) {
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            throw new RuntimeException("Пользователь не найден");
        }
        Long userId = user.getId();
        return gameService.createSinglePlayerGame(userId, request);
    }

    @Operation(summary = "Сделать ход (WebSocket)", description = "Выполняет ход игрока через WebSocket.\n\n**Отправить:** MoveRequest на /app/singleplayer.move\n**Получить:** MoveResponse по /topic/singleplayer/move.\n\nПример запроса:\n```json\n{\n  \"gameId\": 1,\n  \"x\": 5,\n  \"y\": 3\n}\n```\n\nПример ответа MoveResponse см. в /topic/singleplayer/state.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Ход успешно выполнен", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MoveResponse.class)))
    })
    @MessageMapping("/singleplayer.move")
    @SendTo("/topic/singleplayer/move")
    public MoveResponse makeMove(
            @Parameter(description = "Данные хода: ID игры и координаты выстрела", required = true)
            @Payload MoveRequest moveRequest,
            Principal principal) {
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            throw new RuntimeException("Пользователь не найден");
        }
        Long userId = user.getId();
        return gameService.playerMove(userId, moveRequest);
    }

    @Operation(summary = "Получить состояние игры (WebSocket)", description = "Получить текущее состояние игры через WebSocket.\n\n**Отправить:** gameId (Long) на /app/singleplayer.state\n**Получить:** GameDto по /topic/singleplayer/state.\n\nПример запроса:\n```json\n1\n```\n\nПример ответа GameDto см. в /topic/singleplayer/state.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Данные игры успешно получены", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GameDto.class)))
    })
    @MessageMapping("/singleplayer.state")
    @SendTo("/topic/singleplayer/state")
    public GameDto getGameState(
            @Parameter(description = "ID игры", required = true)
            @Payload Long gameId,
            Principal principal) {
        User user = userService.findByUsername(principal.getName());
        if (user == null) {
            throw new RuntimeException("Пользователь не найден");
        }
        Long userId = user.getId();
        return gameService.getGameById(gameId, userId);
    }
}
