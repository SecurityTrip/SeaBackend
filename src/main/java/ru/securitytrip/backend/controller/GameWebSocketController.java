package ru.securitytrip.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import ru.securitytrip.backend.dto.CreateSinglePlayerGameRequest;
import ru.securitytrip.backend.dto.GameDto;
import ru.securitytrip.backend.dto.MoveRequest;
import ru.securitytrip.backend.dto.MoveResponse;
import ru.securitytrip.backend.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "WebSocket Морской Бой (Singleplayer)", description = "WebSocket API для одиночной игры в морской бой. Все методы работают только для singleplayer режима.\n\nКаналы:\n- /app/singleplayer.create (создать игру)\n- /app/singleplayer.move (сделать ход)\n- /app/singleplayer.state (получить состояние)")
@Controller
public class GameWebSocketController {

    @Autowired
    private GameService gameService;

    @Operation(summary = "Создание одиночной игры (WebSocket)",
            description = "Создает новую одиночную игру через WebSocket. Отправьте CreateSinglePlayerGameRequest на /app/singleplayer.create, получите GameDto по /topic/singleplayer/game.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно создана",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GameDto.class)))
    })
    @MessageMapping("/singleplayer.create")
    @SendTo("/topic/singleplayer/game")
    public GameDto createSinglePlayerGame(@Parameter(description = "Данные расстановки кораблей игрока", required = true)
                                          @Payload CreateSinglePlayerGameRequest request,
                                          SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = getUserIdOrGenerateFromUsername(username);
        return gameService.createSinglePlayerGame(userId, request);
    }

    @Operation(summary = "Сделать ход (WebSocket)",
            description = "Выполняет ход игрока через WebSocket. Отправьте MoveRequest на /app/singleplayer.move, получите MoveResponse по /topic/singleplayer/move.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ход успешно выполнен",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MoveResponse.class)))
    })
    @MessageMapping("/singleplayer.move")
    @SendTo("/topic/singleplayer/move")
    public MoveResponse makeMove(@Parameter(description = "Данные хода: ID игры и координаты выстрела", required = true)
                                 @Payload MoveRequest moveRequest,
                                 SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = getUserIdOrGenerateFromUsername(username);
        return gameService.playerMove(userId, moveRequest);
    }

    @Operation(summary = "Получить состояние игры (WebSocket)",
            description = "Получить текущее состояние игры через WebSocket. Отправьте gameId (Long) на /app/singleplayer.state, получите GameDto по /topic/singleplayer/state.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные игры успешно получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = GameDto.class)))
    })
    @MessageMapping("/singleplayer.state")
    @SendTo("/topic/singleplayer/state")
    public GameDto getGameState(@Parameter(description = "ID игры", required = true)
                                @Payload Long gameId,
                                SimpMessageHeaderAccessor headerAccessor) {
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "anonymous";
        Long userId = getUserIdOrGenerateFromUsername(username);
        return gameService.getGameById(gameId, userId);
    }

    private Long getUserIdOrGenerateFromUsername(String username) {
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            return Math.abs((long) username.hashCode());
        }
    }
}
