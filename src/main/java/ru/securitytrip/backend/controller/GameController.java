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
import ru.securitytrip.backend.model.DifficultyLevel;
import ru.securitytrip.backend.service.GameService;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;


/**
 * # WebSocket API для одиночной игры в морской бой
 *
 * ## Подключение
 * ws://<host>:<port>/ws (STOMP/SockJS)
 *
 * ## Каналы и сообщения:
 *
 * - **Создать игру**
 *   - Отправить: /app/singleplayer.create (CreateSinglePlayerGameRequest)
 *   - Получить: /topic/singleplayer/game (GameDto)
 *
 * - **Сделать ход**
 *   - Отправить: /app/singleplayer.move (MoveRequest)
 *   - Получить: /topic/singleplayer/move (MoveResponse)
 *
 * - **Получить состояние игры**
 *   - Отправить: /app/singleplayer.state (gameId: Long)
 *   - Получить: /topic/singleplayer/state (GameDto)
 *
 * Пример работы с WebSocket описан в GameWebSocketController.
 */

@RestController
@RequestMapping("/game")
@CrossOrigin(origins = "http://localhost")
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
        
        // Используем имя пользователя вместо попытки преобразовать его в Long
        Long userId = getUserIdOrGenerateFromUsername(username);
        
        // Создаем игру через сервис
        GameDto gameDto = gameService.createSinglePlayerGame(userId, request);
        
        return ResponseEntity.ok(gameDto);
    }
    
    @Operation(summary = "Создание одиночной игры с автоматической расстановкой кораблей", 
               description = "Создает новую игру с автоматически сгенерированной расстановкой кораблей игрока")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Игра успешно создана",
                    content = @Content(mediaType = "application/json", 
                            schema = @Schema(implementation = GameDto.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Не авторизован",
                    content = @Content)
    })
    @PostMapping("/singleplayer/auto")
    public ResponseEntity<GameDto> createSinglePlayerGameWithAutoPlacement(
            @Parameter(description = "Параметры игры", required = true)
            @RequestBody Map<String, String> request) {
        // Получаем ID текущего пользователя из контекста безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Используем имя пользователя вместо попытки преобразовать его в Long
        Long userId = getUserIdOrGenerateFromUsername(username);
        
        // Получаем уровень сложности (по умолчанию MEDIUM)
        DifficultyLevel difficultyLevel = DifficultyLevel.MEDIUM;
        if (request.containsKey("difficultyLevel")) {
            try {
                difficultyLevel = DifficultyLevel.valueOf(request.get("difficultyLevel").toUpperCase());
            } catch (IllegalArgumentException e) {
                // Если некорректное значение, используем MEDIUM
            }
        }
        
        // Получаем стратегию расстановки (по умолчанию RANDOM)
        String placementStrategy = "RANDOM";
        if (request.containsKey("placementStrategy")) {
            String strategy = request.get("placementStrategy").toUpperCase();
            if (strategy.equals("SHORE") || strategy.equals("ASYMMETRIC") || strategy.equals("RANDOM")) {
                placementStrategy = strategy;
            }
        }
        
        // Генерируем расстановку кораблей
        List<ShipDto> ships = gameService.generatePlayerShips(placementStrategy);
        
        // Обходим проблему с инициализацией объекта
        // Создаем запрос на основе готовых данных
        CreateSinglePlayerGameRequest gameRequest = createGameRequest(ships, difficultyLevel);
        
        // Создаем игру через сервис
        GameDto gameDto = gameService.createSinglePlayerGame(userId, gameRequest);
        
        return ResponseEntity.ok(gameDto);
    }
    
    // Вспомогательный метод для создания объекта запроса
    private CreateSinglePlayerGameRequest createGameRequest(List<ShipDto> ships, DifficultyLevel level) {
        // Создаем новый объект запроса
        CreateSinglePlayerGameRequest request = new CreateSinglePlayerGameRequest();
        
        // Получаем список кораблей
        List<ShipDto> shipsList = new ArrayList<>(ships);
        
        // Используем другой конструктор для обхода проблемы
        return new CreateSinglePlayerGameRequest() {
            @Override
            public List<ShipDto> getShips() {
                return shipsList;
            }
            
            @Override
            public DifficultyLevel getDifficultyLevel() {
                return level;
            }
        };
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
        
        // Используем имя пользователя вместо попытки преобразовать его в Long
        Long userId = getUserIdOrGenerateFromUsername(username);
        
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
        
        // Используем имя пользователя вместо попытки преобразовать его в Long
        Long userId = getUserIdOrGenerateFromUsername(username);
        
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
        
        // Используем имя пользователя вместо попытки преобразовать его в Long
        Long userId = getUserIdOrGenerateFromUsername(username);
        
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
    
    @Operation(summary = "Сгенерировать расстановку кораблей (POST-метод)", 
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
    @PostMapping("/generate-ships")
    public ResponseEntity<List<ShipDto>> generateShipsPost(
            @Parameter(description = "Стратегия расстановки", required = true)
            @RequestBody Map<String, String> request) {
        // Получаем стратегию расстановки (по умолчанию RANDOM)
        String placementStrategy = "RANDOM";
        if (request.containsKey("strategy")) {
            String strategy = request.get("strategy").toUpperCase();
            if (strategy.equals("SHORE") || strategy.equals("ASYMMETRIC") || strategy.equals("RANDOM")) {
                placementStrategy = strategy;
            }
        }
        
        // Генерируем корабли через сервис
        List<ShipDto> ships = gameService.generatePlayerShips(placementStrategy);
        
        return ResponseEntity.ok(ships);
    }

    /**
     * Получает идентификатор пользователя или генерирует его из имени пользователя.
     * Если имя пользователя может быть преобразовано в Long, возвращает его.
     * В противном случае генерирует хеш-код на основе имени пользователя.
     * 
     * @param username Имя пользователя
     * @return Идентификатор пользователя
     */
    private Long getUserIdOrGenerateFromUsername(String username) {
        try {
            // Пытаемся преобразовать имя пользователя в Long
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            // Если не удалось, генерируем положительный хеш на основе имени пользователя
            return Math.abs((long) username.hashCode());
        }
    }
}
