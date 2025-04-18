package ru.securitytrip.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.securitytrip.backend.dto.CreateSinglePlayerGameRequest;
import ru.securitytrip.backend.dto.GameDto;
import ru.securitytrip.backend.dto.MoveRequest;
import ru.securitytrip.backend.dto.MoveResponse;
import ru.securitytrip.backend.dto.ShipDto;
import ru.securitytrip.backend.service.GameService;

import java.util.List;

@RestController
@RequestMapping("/game")
@Tag(name = "Морской Бой", description = "API для управления игрой в морской бой")
public class GameController {

    @Autowired
    private GameService gameService;

    @Operation(summary = "Создание одиночной игры", 
               description = "Создает новую игру в режиме одиночной игры с переданной расстановкой кораблей игрока")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно создана",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = GameDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректная расстановка кораблей",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content)
    })
    @PostMapping("/singleplayer")
    public ResponseEntity<GameDto> createSinglePlayerGame(
            @Parameter(description = "Данные расстановки кораблей игрока", required = true)
            @RequestBody CreateSinglePlayerGameRequest request) {
        // Получаем ID текущего пользователя из контекста безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Преобразуем имя пользователя в ID (в данном случае предполагаем, что это число)
        Long userId = Long.parseLong(username);
        
        // Создаем игру через сервис
        GameDto gameDto = gameService.createSinglePlayerGame(userId, request);
        
        return ResponseEntity.ok(gameDto);
    }
    
    @Operation(summary = "Начало игры", 
               description = "Запускает созданную игру, переводя ее в состояние 'В процессе'")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно запущена",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = GameDto.class))),
            @ApiResponse(responseCode = "400", description = "Игра уже запущена или завершена",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не владелец игры)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Игра не найдена",
                    content = @Content)
    })
    @PostMapping("/start/{gameId}")
    public ResponseEntity<GameDto> startGame(
            @Parameter(description = "Идентификатор игры", required = true)
            @PathVariable Long gameId) {
        // Получаем ID текущего пользователя из контекста безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Преобразуем имя пользователя в ID
        Long userId = Long.parseLong(username);
        
        // Начинаем игру через сервис
        GameDto gameDto = gameService.startGame(gameId, userId);
        
        return ResponseEntity.ok(gameDto);
    }
    
    @Operation(summary = "Сделать ход", 
               description = "Выполняет ход игрока по указанным координатам и автоматически выполняет ход компьютера в случае промаха")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ход успешно выполнен",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = MoveResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные координаты, ход вне очереди или по уже обстрелянной клетке",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Игра не найдена",
                    content = @Content)
    })
    @PostMapping("/move")
    public ResponseEntity<MoveResponse> makeMove(
            @Parameter(description = "Данные хода: ID игры и координаты выстрела", required = true)
            @RequestBody MoveRequest moveRequest) {
        // Получаем ID текущего пользователя из контекста безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Преобразуем имя пользователя в ID
        Long userId = Long.parseLong(username);
        
        // Делаем ход через сервис
        MoveResponse response = gameService.playerMove(userId, moveRequest);
        
        return ResponseEntity.ok(response);
    }
    
    @Operation(summary = "Получить состояние игры", 
               description = "Возвращает текущее состояние указанной игры")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Данные игры успешно получены",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = GameDto.class))),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен (не участник игры)",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Игра не найдена",
                    content = @Content)
    })
    @GetMapping("/{gameId}")
    public ResponseEntity<GameDto> getGame(
            @Parameter(description = "Идентификатор игры", required = true)
            @PathVariable Long gameId) {
        // Получаем ID текущего пользователя из контекста безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Преобразуем имя пользователя в ID
        Long userId = Long.parseLong(username);
        
        // Получаем игру по ID
        GameDto gameDto = gameService.getGameById(gameId, userId);
        
        return ResponseEntity.ok(gameDto);
    }
    
    @Operation(summary = "Сгенерировать расстановку кораблей", 
              description = "Автоматически генерирует расстановку кораблей для игрока по выбранной стратегии")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Расстановка успешно сгенерирована",
                   content = @Content(mediaType = "application/json", 
                           schema = @Schema(implementation = ShipDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректная стратегия",
                   content = @Content),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                   content = @Content)
    })
    @GetMapping("/generate-ships/{strategy}")
    public ResponseEntity<List<ShipDto>> generateShips(
            @Parameter(description = "Стратегия расстановки (RANDOM, SHORE, ASYMMETRIC)", required = true, 
                     schema = @Schema(allowableValues = {"RANDOM", "SHORE", "ASYMMETRIC"}, defaultValue = "RANDOM"))
            @PathVariable String strategy) {
        // Генерируем корабли через сервис
        List<ShipDto> ships = gameService.generatePlayerShips(strategy);
        
        return ResponseEntity.ok(ships);
    }
}
