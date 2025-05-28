package ru.securitytrip.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.securitytrip.backend.dto.*;
import ru.securitytrip.backend.model.*;
import ru.securitytrip.backend.repository.GameBoardRepository;
import ru.securitytrip.backend.repository.GameRepository;
import ru.securitytrip.backend.repository.MultiplayerRoomRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class GameService {

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private GameBoardRepository gameBoardRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private MultiplayerRoomRepository multiplayerRoomRepository;
    
    private final Random random = new Random();
    
    // Кэш для хранения состояния игры
    private final Map<String, GameDto> gameStateCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 1000; // Время жизни кэша в миллисекундах (1 секунда)
    
    // Стандартные размеры кораблей для морского боя
    private static final int[] STANDARD_SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
    
    // Хранилище комнат (gameCode -> MultiplayerRoom)
    private final Map<String, MultiplayerRoom> multiplayerRooms = new ConcurrentHashMap<>();

    // Внутренний класс для хранения состояния комнаты
    private static class MultiplayerRoom {
        Long player1Id;
        List<ShipDto> player1Ships;
        int[][] player1Board; // 0 - пусто, 1 - корабль, 2 - промах, 3 - попадание
        Long player2Id;
        List<ShipDto> player2Ships;
        int[][] player2Board;
        String currentTurn; // "player1" или "player2"
        GameDto gameState;
    }
    
    /**
     * Сохраняет корабли хоста после создания комнаты, если они ещё не были отправлены
     */
    @Transactional
    public GameDto placeHostShips(String gameCode, Long userId, List<ShipDto> ships) {
        MultiplayerRoomEntity entity = multiplayerRoomRepository.findById(gameCode)
            .orElseThrow(() -> new RuntimeException("Комната не найдена"));
        if (!userId.equals(entity.getPlayer1Id())) {
            throw new RuntimeException("Только хост может размещать свои корабли");
        }
        try {
            // Проверяем, не были ли уже размещены корабли
            List<ShipDto> currentShips = entity.getPlayer1ShipsJson() != null
                ? objectMapper.readValue(entity.getPlayer1ShipsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<ShipDto>>() {})
                : new ArrayList<>();
            if (currentShips != null && !currentShips.isEmpty()) {
                throw new RuntimeException("Корабли уже размещены");
            }
            // Гарантируем заполнение positions/hits
            for (ShipDto ship : ships) {
                if (ship.getPositions() == null) {
                    ship.setPositions(generateShipPositionsFromShipDto(ship));
                }
                if (ship.getHits() == null) {
                    ship.setHits(new boolean[ship.getSize()]);
                }
            }
            entity.setPlayer1ShipsJson(objectMapper.writeValueAsString(ships));
            int[][] player1Board = generateBoardWithShips(ships);
            entity.setPlayer1BoardJson(objectMapper.writeValueAsString(player1Board));

            // Если оба игрока уже разместили корабли — IN_PROGRESS, иначе WAITING
            boolean bothReady = entity.getPlayer2ShipsJson() != null && !entity.getPlayer2ShipsJson().equals("[]") && entity.getPlayer2Id() != null;
            GameDto gameState = new GameDto();
            gameState.setMode(GameMode.multiplayer);
            gameState.setGameCode(gameCode);
            if (bothReady) {
                gameState.setGameState(GameState.IN_PROGRESS);
                gameState.setPlayerTurn(true); // Первый ход за хостом
            } else {
                gameState.setGameState(GameState.WAITING);
                gameState.setPlayerTurn(false);
            }

            // playerBoard — поле хоста, computerBoard — поле гостя (пустое, если не готов)
            GameBoardDto playerBoardDto = new GameBoardDto();
            playerBoardDto.setId(entity.getPlayer1Id());
            playerBoardDto.setBoard(player1Board);
            playerBoardDto.setShips(ships);
            playerBoardDto.setComputer(false);

            GameBoardDto computerBoardDto = new GameBoardDto();
            computerBoardDto.setId(entity.getPlayer2Id());
            if (bothReady) {
                int[][] player2Board = objectMapper.readValue(entity.getPlayer2BoardJson(), int[][].class);
                computerBoardDto.setBoard(hideShipsOnBoard(player2Board));
            } else {
                computerBoardDto.setBoard(new int[10][10]);
            }
            computerBoardDto.setShips(new ArrayList<>());
            computerBoardDto.setComputer(true);

            gameState.setPlayerBoard(playerBoardDto);
            gameState.setComputerBoard(computerBoardDto);

            entity.setGameStateJson(objectMapper.writeValueAsString(gameState));
            multiplayerRoomRepository.save(entity);
            return gameState;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при размещении кораблей хоста: " + e.getMessage(), e);
        }
    }

    @Transactional
    public GameDto createSinglePlayerGame(Long userId, CreateSinglePlayerGameRequest request) {
        logger.info("Создание одиночной игры для пользователя: {}", userId);
        
        // Создаем новую игру
        Game game = new Game();
        game.setMode(GameMode.singleplayer);
        game.setDifficultyLevel(request.getDifficultyLevel());
        
        // Создаем доску игрока
        GameBoard playerBoard = new GameBoard();
        playerBoard.setOwnerId(userId);
        playerBoard.setComputer(false);
        playerBoard.initEmptyBoard();
        
        // Устанавливаем корабли игрока на доску
        List<Ship> playerShips = convertToShips(request.getShips());
        try {
            placeShipsOnBoard(playerBoard, playerShips);
            playerBoard.setShipsData(objectMapper.writeValueAsString(playerShips));
        } catch (JsonProcessingException e) {
            logger.error("Ошибка при сериализации кораблей игрока", e);
            throw new RuntimeException("Ошибка при создании игры", e);
        }
        
        // Создаем доску компьютера
        GameBoard computerBoard = new GameBoard();
        computerBoard.setOwnerId(null); // Нет владельца, это ИИ
        computerBoard.setComputer(true);
        computerBoard.initEmptyBoard();
        
        // Генерируем расстановку кораблей для компьютера в зависимости от уровня сложности
        List<Ship> computerShips;
        DifficultyLevel difficultyLevel = request.getDifficultyLevel();
        
        // Используем соответствующую стратегию расстановки
        switch (difficultyLevel) {
            case EASY:
                computerShips = generateShoreStrategy();
                break;
            case MEDIUM:
                computerShips = generateAsymmetricStrategy();
                break;
            case HARD:
                computerShips = generateRandomStrategy();
                break;
            default:
                computerShips = generateRandomStrategy();
        }
        
        try {
            placeShipsOnBoard(computerBoard, computerShips);
            computerBoard.setShipsData(objectMapper.writeValueAsString(computerShips));
        } catch (JsonProcessingException e) {
            logger.error("Ошибка при сериализации кораблей компьютера", e);
            throw new RuntimeException("Ошибка при создании игры", e);
        }
        
        // Связываем доски с игрой
        game.addBoard(playerBoard);
        game.addBoard(computerBoard);
        
        // Сохраняем игру
        game = gameRepository.save(game);
        
        // Преобразуем в DTO для передачи клиенту
        return convertToGameDto(game, userId);
    }
    
    // Метод для преобразования ShipDto из запроса в модель Ship
    private List<Ship> convertToShips(List<ShipDto> shipDtos) {
        return shipDtos.stream()
                .map(dto -> new Ship(dto.getSize(), dto.getX(), dto.getY(), dto.isHorizontal()))
                .collect(Collectors.toList());
    }
    
    // Метод для размещения кораблей на доске и обновления состояния доски
    private void placeShipsOnBoard(GameBoard board, List<Ship> ships) {
        int[][] boardArray = board.getBoardAsArray();
        
        // Размещаем каждый корабль на доске
        for (Ship ship : ships) {
            List<int[]> coordinates = ship.getCoordinates();
            for (int[] coord : coordinates) {
                int x = coord[0];
                int y = coord[1];
                
                // Убедимся, что координаты в пределах доски
                if (x >= 0 && x < 10 && y >= 0 && y < 10) {
                    // Отмечаем клетку как занятую кораблем (1)
                    boardArray[y][x] = 1;
                }
            }
        }
        
        // Обновляем состояние доски
        board.setBoardState(board.convertBoardToString(boardArray));
    }
    
    // Метод для расстановки кораблей по "стратегии берега" (для EASY)
    private List<Ship> generateShoreStrategy() {
        List<Ship> ships = new ArrayList<>();
        int[][] board = new int[10][10]; // 0 - пусто, 1 - корабль, 2 - зона вокруг корабля
        
        // Определяем берег (одну из сторон доски)
        int side = random.nextInt(4); // 0 - верх, 1 - право, 2 - низ, 3 - лево
        
        // Для каждого стандартного размера корабля
        for (int shipSize : STANDARD_SHIP_SIZES) {
            boolean placed = false;
            int attempts = 0;
            
            while (!placed && attempts < 100) {
                int x, y;
                boolean horizontal;
                
                // Генерируем координаты рядом с выбранным берегом
                switch (side) {
                    case 0: // Верхний берег
                        y = random.nextInt(3); // 0-2 строки от верха
                        x = random.nextInt(10);
                        horizontal = true; // На верхнем берегу размещаем корабли горизонтально
                        break;
                    case 1: // Правый берег
                        x = 7 + random.nextInt(3); // 7-9 столбцы от левого края
                        y = random.nextInt(10);
                        horizontal = false; // На правом берегу размещаем корабли вертикально
                        break;
                    case 2: // Нижний берег
                        y = 7 + random.nextInt(3); // 7-9 строки от верха
                        x = random.nextInt(10);
                        horizontal = true; // На нижнем берегу размещаем корабли горизонтально
                        break;
                    case 3: // Левый берег
                    default:
                        x = random.nextInt(3); // 0-2 столбцы от левого края
                        y = random.nextInt(10);
                        horizontal = false; // На левом берегу размещаем корабли вертикально
                        break;
                }
                
                // Пытаемся разместить корабль в этой позиции
                if (canPlaceShip(board, x, y, shipSize, horizontal)) {
                    placeShipOnBoard(board, x, y, shipSize, horizontal);
                    ships.add(new Ship(shipSize, x, y, horizontal));
                    placed = true;
                }
                
                attempts++;
            }
            
            // Если не удалось разместить корабль в береговой зоне,
            // пробуем разместить в случайном месте
            if (!placed) {
                ships.addAll(generateRandomStrategy(board, shipSize));
            }
        }
        
        return ships;
    }
    
    // Метод для асимметричной расстановки кораблей (для MEDIUM)
    private List<Ship> generateAsymmetricStrategy() {
        List<Ship> ships = new ArrayList<>();
        int[][] board = new int[10][10];
        
        // Выбираем "плотную" зону, где будет больше кораблей
        int denseZoneX = random.nextInt(5); // 0-4 для левой части доски
        int denseZoneY = random.nextInt(5); // 0-4 для верхней части доски
        
        // Для каждого стандартного размера корабля
        for (int shipSize : STANDARD_SHIP_SIZES) {
            boolean placed = false;
            int attempts = 0;
            
            while (!placed && attempts < 100) {
                int x, y;
                boolean horizontal = random.nextBoolean();
                
                // Для кораблей размером 3-4 предпочитаем плотную зону
                if (shipSize >= 3 && random.nextInt(10) < 7) { // 70% шанс
                    x = denseZoneX + random.nextInt(5); // плотная зона по X (5x5 клеток)
                    y = denseZoneY + random.nextInt(5); // плотная зона по Y
                } else {
                    // Для остальных - вся доска
                    x = random.nextInt(10);
                    y = random.nextInt(10);
                }
                
                if (canPlaceShip(board, x, y, shipSize, horizontal)) {
                    placeShipOnBoard(board, x, y, shipSize, horizontal);
                    ships.add(new Ship(shipSize, x, y, horizontal));
                    placed = true;
                }
                
                attempts++;
            }
            
            // Если не удалось разместить в асимметричной стратегии
            if (!placed) {
                ships.addAll(generateRandomStrategy(board, shipSize));
            }
        }
        
        return ships;
    }
    
    // Метод для полностью случайной расстановки кораблей (для HARD)
    private List<Ship> generateRandomStrategy() {
        List<Ship> ships = new ArrayList<>();
        int[][] board = new int[10][10]; // 0 - пусто, 1 - корабль, 2 - зона вокруг корабля
        
        // Для каждого стандартного размера корабля
        for (int shipSize : STANDARD_SHIP_SIZES) {
            ships.addAll(generateRandomStrategy(board, shipSize));
        }
        
        return ships;
    }
    
    // Вспомогательный метод для размещения одного корабля случайным образом
    private List<Ship> generateRandomStrategy(int[][] board, int shipSize) {
        List<Ship> result = new ArrayList<>();
        boolean placed = false;
        int attempts = 0;
        
        while (!placed && attempts < 500) {  // Увеличиваем количество попыток с 100 до 500
            int x = random.nextInt(10);
            int y = random.nextInt(10);
            boolean horizontal = random.nextBoolean();
            
            if (canPlaceShip(board, x, y, shipSize, horizontal)) {
                placeShipOnBoard(board, x, y, shipSize, horizontal);
                result.add(new Ship(shipSize, x, y, horizontal));
                placed = true;
            }
            
            attempts++;
        }
        
        if (!placed) {
            // Вместо выброса исключения создаем запасной вариант размещения
            logger.warn("Не удалось разместить корабль размером {} за {} попыток, пробуем очистить часть доски", shipSize, attempts);
            
            // Очищаем некоторые ячейки доски (удаляем зоны вокруг кораблей)
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    if (board[i][j] == 2) { // Если это зона вокруг корабля
                        board[i][j] = 0;    // Очищаем её
                    }
                }
            }
            
            // Пытаемся снова разместить корабль с повышенным лимитом попыток
            attempts = 0;
            while (!placed && attempts < 500) {
                int x = random.nextInt(10);
                int y = random.nextInt(10);
                boolean horizontal = random.nextBoolean();
                
                if (canPlaceShip(board, x, y, shipSize, horizontal)) {
                    placeShipOnBoard(board, x, y, shipSize, horizontal);
                    result.add(new Ship(shipSize, x, y, horizontal));
                    placed = true;
                }
                
                attempts++;
            }
            
            // Если и это не помогло, создаем корабль с минимальным размером в случайном месте
            if (!placed) {
                logger.warn("Не удалось разместить корабль стандартным способом, создаем альтернативный вариант");
                // Создаем корабль меньшего размера (1), который должен поместиться
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 10; j++) {
                        if (board[i][j] == 0) { // Нашли свободную клетку
                            placeShipOnBoard(board, j, i, 1, true);
                            result.add(new Ship(1, j, i, true));
                            placed = true;
                            break;
                        }
                    }
                    if (placed) break;
                }
            }
        }
        
        return result;
    }
    
    // Проверяет, можно ли разместить корабль в указанной позиции
    private boolean canPlaceShip(int[][] board, int x, int y, int size, boolean horizontal) {
        // Проверка выхода за границы поля
        if (horizontal) {
            if (x + size > 10) return false;
        } else {
            if (y + size > 10) return false;
        }
        
        // Проверка клеток под кораблем и вокруг
        for (int i = -1; i <= size; i++) {
            for (int j = -1; j <= 1; j++) {
                int checkX = horizontal ? x + i : x + j;
                int checkY = horizontal ? y + j : y + i;
                
                // Проверяем, что мы не выходим за пределы доски
                if (checkX >= 0 && checkX < 10 && checkY >= 0 && checkY < 10) {
                    // Если клетка занята кораблем (1), нельзя ставить
                    if (board[checkY][checkX] == 1) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    // Размещает корабль на доске
    private void placeShipOnBoard(int[][] board, int x, int y, int size, boolean horizontal) {
        // Отмечаем клетки, занятые кораблем
        for (int i = 0; i < size; i++) {
            if (horizontal) {
                board[y][x + i] = 1;
            } else {
                board[y + i][x] = 1;
            }
        }
        
        // Отмечаем зону вокруг корабля
        for (int i = -1; i <= size; i++) {
            for (int j = -1; j <= 1; j++) {
                int markX = horizontal ? x + i : x + j;
                int markY = horizontal ? y + j : y + i;
                
                // Проверяем, что мы не выходим за пределы доски
                if (markX >= 0 && markX < 10 && markY >= 0 && markY < 10) {
                    // Если клетка не занята кораблем, отмечаем как зону вокруг корабля
                    if (board[markY][markX] == 0) {
                        board[markY][markX] = 2;
                    }
                }
            }
        }
    }
    
    // Конвертирует игру в DTO
    private GameDto convertToGameDto(Game game, Long userId) {
        GameDto dto = new GameDto();
        dto.setId(game.getId());
        dto.setMode(game.getMode());
        dto.setGameState(game.getGameState());
        dto.setPlayerTurn(game.isPlayerTurn());
        
        logger.info("[convertToGameDto] User ID: {}, Game ID: {}, Game Mode: {}", userId, game.getId(), game.getMode());

        for (GameBoard board : game.getBoards()) {
            GameBoardDto boardDto = new GameBoardDto();
            boardDto.setId(board.getId());
            boardDto.setBoard(board.getBoardAsArray());
            boardDto.setComputer(board.isComputer());
            
            logger.info("[convertToGameDto] Processing Board ID: {}, Owner ID: {}, Is Computer: {}", 
                        board.getId(), board.getOwnerId(), board.isComputer());

            try {
                // В одиночной игре определяем доску игрока по признаку isComputer = false
                if (game.getMode() == GameMode.singleplayer) {
                    if (!board.isComputer()) {
                         logger.info("[convertToGameDto] Identified as Singleplayer Player Board");
                         List<Ship> ships = objectMapper.readValue(board.getShipsData(), 
                                 objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
                         boardDto.setShips(ships.stream().map(this::convertToShipDto).collect(Collectors.toList()));
                         dto.setPlayerBoard(boardDto);
                    } else {
                         logger.info("[convertToGameDto] Identified as Singleplayer Computer Board");
                         // Для компьютера показываем только подбитые корабли во время игры, все после игры
                          if (game.getGameState() != GameState.WAITING) {
                             List<Ship> ships = objectMapper.readValue(board.getShipsData(),
                                     objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
                              boardDto.setShips(ships.stream().map(this::convertToShipDto).collect(Collectors.toList()));
                          }
                         dto.setComputerBoard(boardDto);
                    }
                } else if (game.getMode() == GameMode.multiplayer) {
                    // Логика для мультиплеера остается прежней (по userId)
                    if (userId != null && userId.equals(board.getOwnerId())) {
                         logger.info("[convertToGameDto] Identified as Multiplayer Player Board");
                         List<Ship> ships = objectMapper.readValue(board.getShipsData(), 
                                 objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
                         boardDto.setShips(ships.stream().map(this::convertToShipDto).collect(Collectors.toList()));
                         dto.setPlayerBoard(boardDto);
                     } else {
                         logger.info("[convertToGameDto] Identified as Multiplayer Opponent Board");
                         // Временно упрощенная логика для мультиплеера - показываем все корабли противника (для устранения ошибок компиляции)
                          List<Ship> ships = objectMapper.readValue(board.getShipsData(), 
                                  objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
                           boardDto.setShips(ships.stream().map(this::convertToShipDto).collect(Collectors.toList()));
                         dto.setComputerBoard(boardDto); // В мультиплеере поле противника тоже компьютерное с точки зрения DTO
                     }
                }
            } catch (JsonProcessingException e) {
                logger.error("Ошибка при десериализации кораблей", e);
                throw new RuntimeException("Ошибка при получении данных игры", e);
            }
        }
        
        return dto;
    }

    // Конвертирует Ship в ShipDto
    private ShipDto convertToShipDto(Ship ship) {
        ShipDto dto = new ShipDto();
        dto.setSize(ship.getSize());
        dto.setX(ship.getX());
        dto.setY(ship.getY());
        dto.setHorizontal(ship.isHorizontal());
        
        // Преобразуем List<Boolean> в boolean[] и устанавливаем в DTO
        List<Boolean> hitsList = ship.getHits();
        if (hitsList != null) {
            boolean[] hitsArray = new boolean[hitsList.size()];
            for (int i = 0; i < hitsList.size(); i++) {
                Boolean hit = hitsList.get(i);
                hitsArray[i] = hit != null && hit; // Обрабатываем случай null, хотя List<Boolean> обычно не содержит null
            }
            dto.setHits(hitsArray);
        } else {
            // Если hitsList null, создаем пустой массив попаданий соответствующего размера
            dto.setHits(new boolean[ship.getSize()]); 
        }

        // Генерируем и устанавливаем позиции корабля в DTO
        dto.setPositions(generateShipPositionsFromShipDto(dto));

        return dto;
    }

    // Генерация позиций корабля из ShipDto
    private List<int[]> generateShipPositionsFromShipDto(ShipDto ship) {
        List<int[]> positions = new ArrayList<>();
        int size = ship.getSize();
        int x = ship.getX();
        int y = ship.getY();
        boolean isHorizontal = ship.isHorizontal();

        for (int i = 0; i < size; i++) {
            if (isHorizontal) {
                positions.add(new int[]{x + i, y});
            } else {
                positions.add(new int[]{x, y + i});
            }
        }
        return positions;
    }

    // Генерация позиций корабля из Ship модели
    private List<int[]> generateShipPositionsFromShip(Ship ship) {
        List<int[]> positions = new ArrayList<>();
        int x = ship.getX();
        int y = ship.getY();
        for (int i = 0; i < ship.getSize(); i++) {
            if (ship.isHorizontal()) {
                positions.add(new int[]{x + i, y});
            } else {
                positions.add(new int[]{x, y + i});
            }
        }
        return positions;
    }

    @Transactional
    public MoveResponse playerMove(Long userId, MoveRequest moveRequest) {
        // Получаем игру по ID
        Game game = gameRepository.findById(moveRequest.getGameId())
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));
        
        // Проверяем, что игра в процессе
        if (game.getGameState() != GameState.IN_PROGRESS) {
            throw new RuntimeException("Игра уже завершена или еще не начата");
        }
        
        // Проверяем, что сейчас ход игрока
        if (!game.isPlayerTurn()) {
            throw new RuntimeException("Сейчас не ваш ход");
        }
        
        // Получаем доску компьютера, по которой делается ход
        GameBoard computerBoard = game.getComputerBoard();
        
        // Проверяем координаты хода
        if (moveRequest.getX() < 0 || moveRequest.getX() >= 10 || 
            moveRequest.getY() < 0 || moveRequest.getY() >= 10) {
            throw new RuntimeException("Недопустимые координаты хода");
        }
        
        // Получаем текущее состояние доски
        int[][] boardArray = computerBoard.getBoardAsArray();
        
        // Проверяем, не стреляли ли уже по этой клетке
        if (boardArray[moveRequest.getY()][moveRequest.getX()] == 2 || 
            boardArray[moveRequest.getY()][moveRequest.getX()] == 3) {
            throw new RuntimeException("По этой клетке уже стреляли");
        }
        
        // Логируем ход игрока
        logger.info("Ход игрока {} по координатам x={}, y={}", userId, moveRequest.getX(), moveRequest.getY());
        
        // Получаем информацию о кораблях компьютера
        List<Ship> computerShips;
        try {
            computerShips = objectMapper.readValue(computerBoard.getShipsData(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при получении данных о кораблях", e);
        }
        
        // Проверяем, попал ли ход в корабль
        boolean hit = false;
        boolean sunk = false;
        Ship hitShip = null;
        
        for (Ship ship : computerShips) {
            if (ship.hit(moveRequest.getX(), moveRequest.getY())) {
                hit = true;
                hitShip = ship;
                sunk = ship.isSunk();
                break;
            }
        }
        
        // Обновляем состояние доски
        if (hit) {
            // Попадание (3)
            boardArray[moveRequest.getY()][moveRequest.getX()] = 3;
            logger.info("Игрок {} ПОПАЛ по координатам x={}, y={}", userId, moveRequest.getX(), moveRequest.getY());
            if (sunk) {
                logger.info("Игрок {} ПОТОПИЛ корабль размером {} по координатам x={}, y={}", 
                    userId, hitShip.getSize(), moveRequest.getX(), moveRequest.getY());
            }
        } else {
            // Промах (2)
            boardArray[moveRequest.getY()][moveRequest.getX()] = 2;
            logger.info("Игрок {} ПРОМАХНУЛСЯ по координатам x={}, y={}", userId, moveRequest.getX(), moveRequest.getY());
        }
        
        // Обновляем доску
        computerBoard.setBoardState(computerBoard.convertBoardToString(boardArray));
        
        // Обновляем данные о кораблях, если было попадание
        if (hit) {
            try {
                computerBoard.setShipsData(objectMapper.writeValueAsString(computerShips));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Ошибка при сохранении данных о кораблях", e);
            }
        }
        
        // Сохраняем изменения в доске компьютера (даже при промахе)
        try {
            computerBoard.setShipsData(objectMapper.writeValueAsString(computerShips));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Ошибка при сохранении данных о кораблях компьютера", e);
        }
        
        // Проверяем, закончилась ли игра (все ли корабли потоплены)
        boolean gameOver = true;
        for (Ship ship : computerShips) {
            if (!ship.isSunk()) {
                gameOver = false;
                break;
            }
        }
        
        // Если игра закончилась, обновляем статус
        if (gameOver) {
            game.setGameState(GameState.PLAYER_WON);
            logger.info("Игрок ВЫИГРАЛ игру {}", game.getId());
        } else if (!hit) {
            // Если промах, переключаем ход на компьютер
            game.toggleTurn();
            logger.info("Передаём ход компьютеру");
            computerMove(game);
        }
        
        // Сохраняем изменения
        game = gameRepository.save(game);
        
        // Формируем ответ
        MoveResponse response = new MoveResponse(hit, sunk, gameOver);
        
        // Обновляем состояние игры для ответа
        response.setGameState(convertToGameDto(game, userId));
        
        return response;
    }
    
    @Transactional
    public void computerMove(Game game) {
        computerMove(game, 0);
    }
    
    /**
     * Внутренний метод для хода компьютера с ограничением на количество последовательных ходов
     * @param game Текущая игра
     * @param moveCount Счетчик последовательных ходов
     */
    private void computerMove(Game game, int moveCount) {
        // Защита от бесконечной рекурсии - не более 10 последовательных ходов компьютера
        if (moveCount >= 15) {
            logger.warn("Достигнуто максимальное количество последовательных ходов компьютера ({}), передаем ход игроку", moveCount);
            game.toggleTurn();
            gameRepository.save(game);
            return;
        }
        
        // Получаем доску игрока
        GameBoard playerBoard = game.getPlayerBoard();
        
        // Получаем текущее состояние доски
        int[][] boardArray = playerBoard.getBoardAsArray();
        
        // Получаем уровень сложности
        DifficultyLevel difficultyLevel = game.getDifficultyLevel();
        
        // Координаты выстрела
        int[] shotCoordinates = new int[2]; // [x, y]
        boolean validMove = false;
        
        // Выбираем стратегию в зависимости от сложности
        switch (difficultyLevel) {
            case EASY:
                // Полностью случайные выстрелы
                logger.info("Компьютер использует стратегию EASY (случайные выстрелы)");
                validMove = makeRandomShot(boardArray, shotCoordinates);
                break;
                
            case MEDIUM:
                // Выстрелы по шахматной доске
                logger.info("Компьютер использует стратегию MEDIUM (шахматная доска)");
                validMove = makeCheckerboardShot(boardArray, shotCoordinates);
                break;
                
            case HARD:
                // Случайные выстрелы с добиванием кораблей
                logger.info("Компьютер использует стратегию HARD (добивание)");
                validMove = makeSmartShot(boardArray, shotCoordinates, game);
                break;
                
            default:
                // По умолчанию - случайный выстрел
                logger.info("Компьютер использует стратегию по умолчанию");
                validMove = makeRandomShot(boardArray, shotCoordinates);
        }
        
        if (validMove) {
            int x = shotCoordinates[0];
            int y = shotCoordinates[1];
            
            // Логируем ход компьютера
            logger.info("Ход компьютера в игре {} по координатам x={}, y={}", game.getId(), x, y);
            
            // Дополнительная проверка, чтобы убедиться, что клетка еще не обстреляна
            if (boardArray[y][x] == 2 || boardArray[y][x] == 3) {
                logger.error("Ошибка в логике игры: компьютер пытается выстрелить в уже обстреляную клетку x={}, y={}", x, y);
                // Если это происходит, переключаем ход на игрока и завершаем метод
                game.toggleTurn();
                gameRepository.save(game);
                return;
            }
            
            // Получаем информацию о кораблях игрока
            List<Ship> playerShips;
            try {
                playerShips = objectMapper.readValue(playerBoard.getShipsData(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Ошибка при получении данных о кораблях игрока", e);
            }
            
            // Проверяем, попал ли ход в корабль
            boolean hit = false;
            Ship hitShip = null;
            
            for (Ship ship : playerShips) {
                if (ship.hit(x, y)) {
                    hit = true;
                    hitShip = ship;
                    break;
                }
            }
            
            // Обновляем состояние доски
            if (hit) {
                // Попадание (3)
                boardArray[y][x] = 3;
                logger.info("Компьютер ПОПАЛ по координатам x={}, y={}", x, y);
                
                // Проверяем, потоплен ли корабль
                boolean isShipSunk = true;
                for (boolean hitStatus : hitShip.getHits()) {
                    if (!hitStatus) {
                        isShipSunk = false;
                        break;
                    }
                }

                // Если корабль потоплен, помечаем соседние клетки как промах
                if (isShipSunk) {
                    for (int[] position : hitShip.getCoordinates()) {
                        int shipX = position[0];
                        int shipY = position[1];
                        
                        // Проверяем все соседние клетки
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                int nx = shipX + dx;
                                int ny = shipY + dy;
                                
                                // Проверяем границы поля
                                if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                                    // Если клетка пустая (0), помечаем её как промах (2)
                                    if (boardArray[ny][nx] < 2) {
                                        boardArray[ny][nx] = 2;
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Промах (2)
                boardArray[y][x] = 2;
                logger.info("Компьютер ПРОМАХНУЛСЯ по координатам x={}, y={}", x, y);
            }
            
            // Обновляем доску
            playerBoard.setBoardState(playerBoard.convertBoardToString(boardArray));
            

            // Если был HARD уровень и попадание, добавляем в pendingHits
            if (difficultyLevel == DifficultyLevel.HARD && hit) {
                // Добавляем попадание, если оно не потоплено
                boolean isShipSunk = true;
                for (boolean hitStatus : hitShip.getHits()) {
                    if (!hitStatus) {
                        isShipSunk = false;
                        break;
                    }
                }
                if (!isShipSunk) {
                    boolean already = false;
                    for (Game.PendingHit ph : game.getPendingHits()) {
                        if (ph.getX() == x && ph.getY() == y) {
                            already = true;
                            break;
                        }
                    }
                    if (!already) {
                        game.getPendingHits().add(new Game.PendingHit(x, y));
                    }
                }
                game.setLastHitX(x);
                game.setLastHitY(y);
            }
            
            // Обновляем данные о кораблях, если было попадание
            if (hit) {
                try {
                    playerBoard.setShipsData(objectMapper.writeValueAsString(playerShips));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Ошибка при сохранении данных о кораблях игрока", e);
                }
                
                // Проверяем, закончилась ли игра (все ли корабли потоплены)
                boolean gameOver = true;
                for (Ship ship : playerShips) {
                    if (!ship.isSunk()) {
                        gameOver = false;
                        break;
                    }
                }
                // Если корабль потоплен, удаляем все связанные pendingHits
                boolean isShipSunk = true;
                for (boolean hitStatus : hitShip.getHits()) {
                    if (!hitStatus) {
                        isShipSunk = false;
                        break;
                    }
                }
                if (isShipSunk && difficultyLevel == DifficultyLevel.HARD) {
                    List<int[]> coords = hitShip.getCoordinates();
                    game.getPendingHits().removeIf(ph -> {
                        for (int[] c : coords) {
                            if (ph.getX() == c[0] && ph.getY() == c[1]) return true;
                        }
                        return false;
                    });
                }
                // Если игра закончилась, обновляем статус
                if (gameOver) {
                    game.setGameState(GameState.COMPUTER_WON);
                    logger.info("Компьютер ВЫИГРАЛ игру {}", game.getId());
                } else {
                    // Если попал, компьютер ходит еще раз (рекурсивно) с увеличением счетчика ходов
                    gameRepository.save(game);
                    computerMove(game, moveCount + 1);
                    return;
                }
            } else {
                // Если промах на HARD, не сбрасываем pendingHits, но сбрасываем lastHitX/lastHitY
                if (difficultyLevel == DifficultyLevel.HARD) {
                    game.setLastHitX(-1);
                    game.setLastHitY(-1);
                }
                // Если промах, переключаем ход на игрока
                game.toggleTurn();
            }
            
            // Сохраняем изменения
            gameRepository.save(game);
        }
    }
    
    // Метод для случайного выстрела (для EASY)
    private boolean makeRandomShot(int[][] boardArray, int[] coordinates) {
        boolean validMove = false;
        int attempts = 0;
        int maxAttempts = 500; // Предотвращение бесконечного цикла
        
        while (!validMove && attempts < maxAttempts) {
            int x = random.nextInt(10);
            int y = random.nextInt(10);
            
            // Проверяем, что по этой клетке еще не стреляли
            if (boardArray[y][x] != 2 && boardArray[y][x] != 3) {
                coordinates[0] = x;
                coordinates[1] = y;
                validMove = true;
            }
            
            attempts++;
        }
        
        if (!validMove) {
            logger.warn("Не удалось найти клетку для случайного выстрела после {} попыток", maxAttempts);
        }
        
        return validMove;
    }
    
    // Метод для выстрела по шахматной доске (для MEDIUM)
    private boolean makeCheckerboardShot(int[][] boardArray, int[] coordinates) {
        boolean validMove = false;
        
        // Попытки найти ход по шахматному паттерну
        for (int attempts = 0; attempts < 100 && !validMove; attempts++) {
            int x = random.nextInt(10);
            int y = random.nextInt(10);
            
            // Проверяем, что клетка следует шахматному паттерну
            // (сумма координат четная), и по ней еще не стреляли
            if ((x + y) % 2 == 0 && boardArray[y][x] != 2 && boardArray[y][x] != 3) {
                coordinates[0] = x;
                coordinates[1] = y;
                validMove = true;
            }
        }
        
        // Если не удалось найти ход по шахматному паттерну, делаем случайный ход
        if (!validMove) {
            logger.info("Не удалось найти подходящую клетку по шахматной стратегии, переключаемся на случайную");
            return makeRandomShot(boardArray, coordinates);
        }
        
        return validMove;
    }
    
    // Метод для умного выстрела с добиванием кораблей (для HARD)
    private boolean makeSmartShot(int[][] boardArray, int[] coordinates, Game game) {
        boolean validMove = false;
        // Если есть "висящие" попадания, пытаемся добить их
        List<Game.PendingHit> pendingHits = game.getPendingHits();
        outer:
        for (Game.PendingHit ph : pendingHits) {
            int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            for (int[] dir : directions) {
                int newX = ph.getX() + dir[0];
                int newY = ph.getY() + dir[1];
                if (newX >= 0 && newX < 10 && newY >= 0 && newY < 10 &&
                        boardArray[newY][newX] != 2 && boardArray[newY][newX] != 3) {
                    coordinates[0] = newX;
                    coordinates[1] = newY;
                    validMove = true;
                    break outer;
                }
            }
        }
        // Если нет pendingHits или не нашли подходящую клетку, используем lastHitX/lastHitY
        if (!validMove && game.getLastHitX() >= 0 && game.getLastHitY() >= 0) {
            int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            for (int[] dir : directions) {
                int newX = game.getLastHitX() + dir[0];
                int newY = game.getLastHitY() + dir[1];
                if (newX >= 0 && newX < 10 && newY >= 0 && newY < 10 &&
                        boardArray[newY][newX] != 2 && boardArray[newY][newX] != 3) {
                    coordinates[0] = newX;
                    coordinates[1] = newY;
                    validMove = true;
                    break;
                }
            }
        }
        // Если не удалось найти умный ход, делаем случайный
        if (!validMove) {
            logger.info("Не удалось найти подходящую клетку для добивания, переключаемся на случайную");
            return makeRandomShot(boardArray, coordinates);
        }
        return validMove;
    }
    
    @Transactional
    public GameDto startGame(Long gameId, Long userId) {
        // Получаем игру по ID
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));
        
        // Проверяем, что игра в статусе ожидания
        if (game.getGameState() != GameState.WAITING) {
            throw new RuntimeException("Игра уже начата или завершена");
        }
        
        // Проверяем, что пользователь является владельцем игры
        GameBoard playerBoard = game.getPlayerBoard();
        if (playerBoard == null || !userId.equals(playerBoard.getOwnerId())) {
            throw new RuntimeException("Вы не являетесь владельцем этой игры");
        }
        
        // Начинаем игру
        game.setGameState(GameState.IN_PROGRESS);
        game.setPlayerTurn(true); // Первый ход за игроком
        
        // Сохраняем изменения
        game = gameRepository.save(game);
        
        // Возвращаем обновленное состояние игры
        return convertToGameDto(game, userId);
    }

    @Transactional(readOnly = true)
    public GameDto getGameById(Long gameId, Long userId) {
        // Получаем игру по ID
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new RuntimeException("Игра не найдена"));
        
        // Возвращаем данные игры
        return convertToGameDto(game, userId);
    }
    
    /**
     * Генерирует случайную расстановку кораблей для игрока
     * @param placementStrategy Стратегия расстановки: "RANDOM", "SHORE", "ASYMMETRIC"
     * @return Список кораблей, сгенерированных согласно выбранной стратегии
     */
    @Transactional(readOnly = true)
    public List<ShipDto> generatePlayerShips(String placementStrategy) {
        List<Ship> ships;
        
        // Выбираем стратегию расстановки
        switch (placementStrategy.toUpperCase()) {
            case "SHORE":
                ships = generateShoreStrategy();
                break;
            case "ASYMMETRIC":
                ships = generateAsymmetricStrategy();
                break;
            case "RANDOM":
            default:
                ships = generateRandomStrategy();
                break;
        }
        
        // Преобразуем Ship в ShipDto для передачи клиенту
        return ships.stream().map(this::convertToShipDto).collect(Collectors.toList());
    }

    // Helper method to generate the board array with ships placed
    private int[][] generateBoardWithShips(List<ShipDto> ships) {
        int[][] board = new int[10][10]; // Initialize with zeros
        for (ShipDto ship : ships) {
            // Ensure positions are generated if not already present
            if (ship.getPositions() == null) {
                ship.setPositions(generateShipPositionsFromShipDto(ship));
            }
            for (int[] pos : ship.getPositions()) {
                int x = pos[0];
                int y = pos[1];
                // Mark the cell as occupied by a ship (using 1)
                if (x >= 0 && x < 10 && y >= 0 && y < 10) {
                    board[y][x] = 1;
                }
            }
        }
        return board;
    }

    // Создание мультиплеерной игры, возвращает сгенерированный код
    @Transactional
    public String createMultiplayerGame(Long userId, List<ShipDto> ships) {
        String code = java.util.UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        logger.info("Создаю комнату с кодом: {}", code);
        MultiplayerRoomEntity entity = new MultiplayerRoomEntity();
        entity.setCode(code);
        entity.setPlayer1Id(userId);
        entity.setCurrentTurn("player1");
        try {
            // Инициализируем пустые доски и корабли для обоих игроков
            int[][] emptyBoard = new int[10][10];
            List<ShipDto> emptyShips = new ArrayList<>();
            // Не сохраняем корабли хоста на этом этапе!
            entity.setPlayer1ShipsJson(objectMapper.writeValueAsString(emptyShips));
            entity.setPlayer1BoardJson(objectMapper.writeValueAsString(emptyBoard));
            // --- НЕ сохраняем пустые данные для игрока 2 ---
            // entity.setPlayer2ShipsJson(objectMapper.writeValueAsString(emptyShips));
            // entity.setPlayer2BoardJson(objectMapper.writeValueAsString(emptyBoard));

            // Инициализация GameDto с начальным состоянием (ожидание игрока 2 и расстановки хоста)
            GameDto gameState = new GameDto();
            gameState.setMode(GameMode.multiplayer);
            gameState.setGameState(GameState.WAITING);
            gameState.setPlayerTurn(false); // Игра не началась, ход не активен

            // Для хоста его поле - playerBoard, поле противника пока пустое - computerBoard
            GameBoardDto hostBoardDto = new GameBoardDto();
            hostBoardDto.setId(entity.getPlayer1Id()); // Устанавливаем id игрока 1
            hostBoardDto.setBoard(emptyBoard);
            hostBoardDto.setShips(emptyShips);
            hostBoardDto.setComputer(false);

            GameBoardDto emptyOpponentBoardDto = new GameBoardDto();
            emptyOpponentBoardDto.setId(entity.getPlayer2Id()); // Устанавливаем id игрока 2
            emptyOpponentBoardDto.setBoard(emptyBoard);
            emptyOpponentBoardDto.setShips(emptyShips);
            emptyOpponentBoardDto.setComputer(true);

            gameState.setPlayerBoard(hostBoardDto);
            gameState.setComputerBoard(emptyOpponentBoardDto);

            // Сохраняем состояние игры
            entity.setGameStateJson(objectMapper.writeValueAsString(gameState));

            // Сохраняем режим игры в базе данных
            entity.setGameMode(GameMode.multiplayer);

            // Сохраняем и фиксируем изменения
            entity = multiplayerRoomRepository.save(entity);
            multiplayerRoomRepository.flush();

            logger.info("Комната {} успешно создана и сохранена в БД", code);
            return code;

        } catch (Exception e) {
            logger.error("Ошибка при создании мультиплеерной игры: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при создании мультиплеерной игры: " + e.getMessage(), e);
        }
    }

    // Подключение к игре по коду, возвращает DTO игры
    @Transactional
    public GameDto joinMultiplayerGame(String gameCode, Long userId, List<ShipDto> ships) {
        MultiplayerRoomEntity entity = multiplayerRoomRepository.findById(gameCode)
            .orElseThrow(() -> new RuntimeException("Комната не найдена или уже заполнена"));
        if (entity.getPlayer2Id() != null) {
            throw new RuntimeException("Комната уже заполнена");
        }
        entity.setPlayer2Id(userId);
        try {
            // --- ГАРАНТИРУЕМ заполнение hits и positions ---
            for (ShipDto ship : ships) {
                if (ship.getPositions() == null) {
                    ship.setPositions(generateShipPositionsFromShipDto(ship));
                }
                if (ship.getHits() == null) {
                    ship.setHits(new boolean[ship.getSize()]);
                }
            }
            entity.setPlayer2ShipsJson(objectMapper.writeValueAsString(ships));
            int[][] player2Board = generateBoardWithShips(ships);
            entity.setPlayer2BoardJson(objectMapper.writeValueAsString(player2Board));

            // Получаем доски и корабли игрока 1 из сущности
            List<ShipDto> player1Ships = objectMapper.readValue(entity.getPlayer1ShipsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<ShipDto>>() {});
            int[][] player1Board = objectMapper.readValue(entity.getPlayer1BoardJson(), int[][].class);

            // --- GameDto для игрока 2 ---
            GameDto player2GameState = new GameDto();
            player2GameState.setId(userId); // Устанавливаем id игрока
            player2GameState.setMode(GameMode.multiplayer);
            player2GameState.setGameState(GameState.IN_PROGRESS);
            player2GameState.setPlayerTurn(false); // Первый ход за игроком 1
            player2GameState.setGameCode(gameCode); // Устанавливаем код игры

            GameBoardDto player2BoardDto = new GameBoardDto();
            player2BoardDto.setId(entity.getPlayer2Id()); // Устанавливаем id игрока 2
            player2BoardDto.setBoard(player2Board);
            player2BoardDto.setShips(ships);
            player2BoardDto.setComputer(false);

            GameBoardDto player1BoardDto = new GameBoardDto();
            player1BoardDto.setId(entity.getPlayer1Id()); // Устанавливаем id игрока 1
            player1BoardDto.setBoard(hideShipsOnBoard(player1Board));
            player1BoardDto.setShips(new ArrayList<>());
            player1BoardDto.setComputer(true);

            player2GameState.setPlayerBoard(player2BoardDto);
            player2GameState.setComputerBoard(player1BoardDto);

            // --- GameDto для игрока 1 ---
            GameDto player1GameState = new GameDto();
            player1GameState.setId(entity.getPlayer1Id()); // Устанавливаем id игрока 1
            player1GameState.setMode(GameMode.multiplayer);
            player1GameState.setGameState(GameState.IN_PROGRESS);
            player1GameState.setPlayerTurn(true); // Первый ход за игроком 1
            player1GameState.setGameCode(gameCode); // Устанавливаем код игры

            GameBoardDto player1BoardDto2 = new GameBoardDto();
            player1BoardDto2.setId(entity.getPlayer1Id()); // Устанавливаем id игрока 1
            player1BoardDto2.setBoard(player1Board);
            player1BoardDto2.setShips(player1Ships);
            player1BoardDto2.setComputer(false);

            GameBoardDto player2BoardDto2 = new GameBoardDto();
            player2BoardDto2.setId(entity.getPlayer2Id()); // Устанавливаем id игрока 2
            player2BoardDto2.setBoard(hideShipsOnBoard(player2Board));
            player2BoardDto2.setShips(new ArrayList<>());
            player2BoardDto2.setComputer(true);

            player1GameState.setPlayerBoard(player1BoardDto2);
            player1GameState.setComputerBoard(player2BoardDto2);

            // Сохраняем состояние игры для обоих игроков
            entity.setGameStateJson(objectMapper.writeValueAsString(player1GameState));
            entity.setCurrentTurn("player1"); // Первый ход за игроком 1

            // Сохраняем изменения в базе данных
            entity = multiplayerRoomRepository.save(entity);
            multiplayerRoomRepository.flush();

            // Возвращаем состояние для игрока 2
            return player2GameState;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации/десериализации комнаты", e);
        }
    }

    @Transactional
    public GameDto makeMultiplayerMove(String gameCode, Long userId, MoveRequest moveRequest) {
        MultiplayerRoomEntity entity = multiplayerRoomRepository.findById(gameCode)
            .orElseThrow(() -> new RuntimeException("Комната не найдена"));

        if (entity.getPlayer2Id() == null) {
            throw new RuntimeException("Ожидание второго игрока");
        }

        boolean isPlayer1 = userId.equals(entity.getPlayer1Id());
        boolean isPlayer2 = userId.equals(entity.getPlayer2Id());
        if (!isPlayer1 && !isPlayer2) {
            throw new RuntimeException("Вы не участник этой игры");
        }

        try {
            GameDto gameState = objectMapper.readValue(entity.getGameStateJson(), GameDto.class);
            if (gameState.getGameState() != GameState.IN_PROGRESS) {
                throw new RuntimeException("Игра не в процессе");
            }

            // Проверяем, чей сейчас ход
            String currentTurn = entity.getCurrentTurn();
            if ((isPlayer1 && !"player1".equals(currentTurn)) || 
                (isPlayer2 && !"player2".equals(currentTurn))) {
                throw new RuntimeException("Сейчас не ваш ход");
            }

            int[][] player1Board = objectMapper.readValue(entity.getPlayer1BoardJson(), int[][].class);
            int[][] player2Board = objectMapper.readValue(entity.getPlayer2BoardJson(), int[][].class);
            List<ShipDto> player1Ships = objectMapper.readValue(entity.getPlayer1ShipsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<ShipDto>>() {});
            List<ShipDto> player2Ships = objectMapper.readValue(entity.getPlayer2ShipsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<ShipDto>>() {});

            // Определяем, по чьей доске стреляем
            int[][] targetBoard = isPlayer1 ? player2Board : player1Board;
            List<ShipDto> targetShips = isPlayer1 ? player2Ships : player1Ships;

            // Проверяем, не стреляли ли уже по этой клетке
            if (targetBoard[moveRequest.getY()][moveRequest.getX()] == 2 || 
                targetBoard[moveRequest.getY()][moveRequest.getX()] == 3) {
                throw new RuntimeException("По этой клетке уже стреляли");
            }

            // Проверяем попадание
            boolean hit = false;
            boolean sunk = false;
            boolean gameOver = false;

            for (ShipDto ship : targetShips) {
                if (ship.getX() <= moveRequest.getX() && moveRequest.getX() < ship.getX() + (ship.isHorizontal() ? ship.getSize() : 1) &&
                    ship.getY() <= moveRequest.getY() && moveRequest.getY() < ship.getY() + (ship.isHorizontal() ? 1 : ship.getSize())) {
                    hit = true;
                    // Находим индекс попадания в массиве hits
                    int hitIndex = ship.isHorizontal() ? 
                        moveRequest.getX() - ship.getX() : 
                        moveRequest.getY() - ship.getY();
                    boolean[] hits = ship.getHits();
                    hits[hitIndex] = true;
                    ship.setHits(hits);
                    // Проверяем, потоплен ли корабль
                    sunk = true;
                    for (boolean h : hits) {
                        if (!h) {
                            sunk = false;
                            break;
                        }
                    }
                    break;
                }
            }

            // Обновляем доску
            targetBoard[moveRequest.getY()][moveRequest.getX()] = hit ? 3 : 2;

            // Проверяем, потоплен ли корабль, который был только что подбит
            ShipDto hitShipDto = null;
            if (hit) {
                 for (ShipDto ship : targetShips) {
                     if (ship.getX() <= moveRequest.getX() && moveRequest.getX() < ship.getX() + (ship.isHorizontal() ? ship.getSize() : 1) &&
                         ship.getY() <= moveRequest.getY() && moveRequest.getY() < ship.getY() + (ship.isHorizontal() ? 1 : ship.getSize())) {
                         hitShipDto = ship;
                         break;
                     }
                 }
             }

            if (hitShipDto != null) {
                 boolean isShipSunkAfterHit = true;
                 boolean[] hitsAfterHit = hitShipDto.getHits();
                 for (boolean h : hitsAfterHit) {
                     if (!h) {
                         isShipSunkAfterHit = false;
                         break;
                     }
                 }
                 sunk = isShipSunkAfterHit; // Обновляем флаг sunk на основе текущего подбитого корабля

                 // Если корабль потоплен, помечаем соседние клетки как промах
                 if (sunk) {
                      for (int[] position : generateShipPositionsFromShipDto(hitShipDto)) {
                          int shipX = position[0];
                          int shipY = position[1];

                          // Проверяем все соседние клетки
                          for (int dx = -1; dx <= 1; dx++) {
                              for (int dy = -1; dy <= 1; dy++) {
                                  int nx = shipX + dx;
                                  int ny = shipY + dy;

                                  // Проверяем границы поля
                                  if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                                      // Если клетка пустая (0), помечаем её как промах (2)
                                      if (targetBoard[ny][nx] < 2) {
                                          targetBoard[ny][nx] = 2;
                                      }
                                  }
                              }
                          }
                      }
                  }
             }

            // Проверяем, закончилась ли игра
            gameOver = targetShips.stream().allMatch(ship -> {
                boolean[] hits = ship.getHits();
                for (boolean h : hits) {
                    if (!h) return false;
                }
                return true;
            });

            // Обновляем состояние игры
            if (gameOver) {
                gameState.setGameState(isPlayer1 ? GameState.PLAYER_WON : GameState.COMPUTER_WON);
            } else if (!hit) {
                // Переключаем ход только при промахе
                entity.setCurrentTurn(isPlayer1 ? "player2" : "player1");
            }

            // Сохраняем обновленные данные
            if (isPlayer1) {
                entity.setPlayer2BoardJson(objectMapper.writeValueAsString(targetBoard));
                entity.setPlayer2ShipsJson(objectMapper.writeValueAsString(targetShips));
            } else {
                entity.setPlayer1BoardJson(objectMapper.writeValueAsString(targetBoard));
                entity.setPlayer1ShipsJson(objectMapper.writeValueAsString(targetShips));
            }

            // Обновляем состояние игры для отображения
            gameState.setPlayerTurn("player1".equals(entity.getCurrentTurn()));
            gameState.setGameState(gameState.getGameState());

            // Обновляем доски для отображения
            GameBoardDto playerBoardDto = new GameBoardDto();
            playerBoardDto.setId(isPlayer1 ? entity.getPlayer1Id() : entity.getPlayer2Id());
            playerBoardDto.setBoard(isPlayer1 ? player1Board : player2Board);
            playerBoardDto.setShips(isPlayer1 ? player1Ships : player2Ships);
            playerBoardDto.setComputer(false);

            GameBoardDto opponentBoardDto = new GameBoardDto();
            opponentBoardDto.setId(isPlayer1 ? entity.getPlayer2Id() : entity.getPlayer1Id());
            opponentBoardDto.setBoard(hideShipsOnBoard(isPlayer1 ? player2Board : player1Board));
            opponentBoardDto.setShips(new ArrayList<>());
            opponentBoardDto.setComputer(true);

            gameState.setPlayerBoard(playerBoardDto);
            gameState.setComputerBoard(opponentBoardDto);
            gameState.setMode(GameMode.multiplayer);
            gameState.setGameCode(gameCode);

            // Сохраняем обновленное состояние
            entity.setGameStateJson(objectMapper.writeValueAsString(gameState));
            multiplayerRoomRepository.save(entity);

            // Добавляем информацию о результате хода
            gameState.setLastMoveHit(hit);
            gameState.setLastMoveSunk(sunk);
            gameState.setLastMoveGameOver(gameOver);

            return gameState;

        } catch (Exception e) {
            throw new RuntimeException("Ошибка при выполнении хода: " + e.getMessage(), e);
        }
    }
    
    @Transactional(readOnly = true)
    public GameDto getMultiplayerGameState(String gameCode) {
        logger.info("Ищу комнату с кодом: {}", gameCode);
        String cleanCode;
        try {
            // Проверяем кэш
            GameDto cachedState = gameStateCache.get(gameCode);
            Long lastUpdate = lastUpdateTime.get(gameCode);
            if (cachedState != null && lastUpdate != null && 
                System.currentTimeMillis() - lastUpdate < CACHE_TTL) {
                return cachedState;
            }
            
            // Пытаемся распарсить JSON, если это JSON строка
            Map<String, String> jsonMap = objectMapper.readValue(gameCode, Map.class);
            cleanCode = jsonMap.get("gameCode");
            if (cleanCode == null) {
                throw new RuntimeException("Неверный формат кода комнаты");
            }
        } catch (Exception e) {
            // Если не JSON, используем как есть
            cleanCode = gameCode.trim().replaceAll("^[\"']|[\"']$", "");
        }
        logger.info("Очищенный код комнаты: {}", cleanCode);
        
        MultiplayerRoomEntity entity = multiplayerRoomRepository.findById(cleanCode)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));
        
        try {
            GameDto gameState = objectMapper.readValue(entity.getGameStateJson(), GameDto.class);
            // Устанавливаем режим игры из сущности
            gameState.setMode(entity.getGameMode());
            gameState.setGameCode(cleanCode); // Добавляем установку gameCode
            
            // Сохраняем в кэш
            gameStateCache.put(gameCode, gameState);
            lastUpdateTime.put(gameCode, System.currentTimeMillis());
            
            return gameState;
        } catch (Exception e) {
            logger.error("Ошибка при десериализации состояния игры: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка при получении состояния игры", e);
        }
    }

    @Transactional(readOnly = true)
    public GameDto getMultiplayerGameState(String gameCode, Long userId) {
        logger.info("Ищу комнату с кодом: {} для userId={}", gameCode, userId);
        
        // Проверяем кэш
        String cacheKey = gameCode + "_" + userId; // Добавляем userId в ключ кэша
        GameDto cachedState = gameStateCache.get(cacheKey);
        Long lastUpdate = lastUpdateTime.get(cacheKey);
        if (cachedState != null && lastUpdate != null && 
            System.currentTimeMillis() - lastUpdate < CACHE_TTL) {
            return cachedState;
        }
        

        String cleanCode = gameCode;
        MultiplayerRoomEntity entity = multiplayerRoomRepository.findById(cleanCode)
                .orElseThrow(() -> new RuntimeException("Комната не найдена"));
        try {
            // Получаем корабли и доски обоих игроков
            List<ShipDto> player1Ships = entity.getPlayer1ShipsJson() != null ?
                objectMapper.readValue(entity.getPlayer1ShipsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<ShipDto>>() {}) : new ArrayList<>();
            int[][] player1Board = entity.getPlayer1BoardJson() != null ?
                objectMapper.readValue(entity.getPlayer1BoardJson(), int[][].class) : new int[10][10];
            List<ShipDto> player2Ships = entity.getPlayer2ShipsJson() != null ?
                objectMapper.readValue(entity.getPlayer2ShipsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<ShipDto>>() {}) : new ArrayList<>();
            int[][] player2Board = entity.getPlayer2BoardJson() != null ?
                objectMapper.readValue(entity.getPlayer2BoardJson(), int[][].class) : new int[10][10];

            boolean isPlayer1 = userId != null && userId.equals(entity.getPlayer1Id());
            boolean isPlayer2 = userId != null && entity.getPlayer2Id() != null && userId.equals(entity.getPlayer2Id());

            GameDto gameState = new GameDto();
            gameState.setId(userId);
            gameState.setMode(GameMode.multiplayer);
            gameState.setGameCode(cleanCode);

            // --- Добавляем информацию о противнике ---
            Long opponentId = null;
            if (isPlayer1 && entity.getPlayer2Id() != null) {
                opponentId = entity.getPlayer2Id();
            } else if (isPlayer2) {
                opponentId = entity.getPlayer1Id();
            }
            if (opponentId != null) {
                try {
                    ru.securitytrip.backend.model.User opponent = userService.findById(opponentId);
                    if (opponent != null) {
                        gameState.setOpponentUsername(opponent.getUsername());
                        gameState.setOpponentAvatarId(opponent.getAvatarId());
                    }
                } catch (Exception e) {
                    logger.warn("Не удалось получить данные противника: {}", e.getMessage());
                }
            }

            if (entity.getPlayer2Id() == null) {
                gameState.setGameState(GameState.WAITING);
                gameState.setPlayerTurn(false);
            } else {
                gameState.setGameState(GameState.IN_PROGRESS);
                String currentTurn = entity.getCurrentTurn();
                if (isPlayer1) {
                    gameState.setPlayerTurn("player1".equals(currentTurn));
                } else if (isPlayer2) {
                    gameState.setPlayerTurn("player2".equals(currentTurn));
                } else {
                    gameState.setPlayerTurn(false);
                }
            }

            // Формируем состояние игры в зависимости от того, кто запрашивает
            if (isPlayer1) {
                // Для игрока 1 (хоста)
                GameBoardDto playerBoardDto = new GameBoardDto();
                playerBoardDto.setId(entity.getPlayer1Id());
                playerBoardDto.setBoard(player1Board);
                playerBoardDto.setShips(player1Ships);
                playerBoardDto.setComputer(false);

                GameBoardDto opponentBoardDto = new GameBoardDto();
                opponentBoardDto.setId(entity.getPlayer2Id());
                opponentBoardDto.setBoard(hideShipsOnBoard(player2Board));
                opponentBoardDto.setShips(new ArrayList<>());
                opponentBoardDto.setComputer(true);

                gameState.setPlayerBoard(playerBoardDto);
                gameState.setComputerBoard(opponentBoardDto);
            } else if (isPlayer2) {
                // Для игрока 2
                GameBoardDto playerBoardDto = new GameBoardDto();
                playerBoardDto.setId(entity.getPlayer2Id());
                playerBoardDto.setBoard(player2Board);
                playerBoardDto.setShips(player2Ships);
                playerBoardDto.setComputer(false);

                GameBoardDto opponentBoardDto = new GameBoardDto();
                opponentBoardDto.setId(entity.getPlayer1Id());
                opponentBoardDto.setBoard(hideShipsOnBoard(player1Board));
                opponentBoardDto.setShips(new ArrayList<>());
                opponentBoardDto.setComputer(true);

                gameState.setPlayerBoard(playerBoardDto);
                gameState.setComputerBoard(opponentBoardDto);
            } else {
                // Для неизвестного пользователя
                GameBoardDto emptyPlayerBoard = new GameBoardDto();
                emptyPlayerBoard.setId(userId);
                emptyPlayerBoard.setBoard(new int[10][10]);
                emptyPlayerBoard.setShips(new ArrayList<>());
                emptyPlayerBoard.setComputer(false);
                
                GameBoardDto emptyOpponentBoard = new GameBoardDto();
                emptyOpponentBoard.setId(null);
                emptyOpponentBoard.setBoard(new int[10][10]);
                emptyOpponentBoard.setShips(new ArrayList<>());
                emptyOpponentBoard.setComputer(true);
                
                gameState.setPlayerBoard(emptyPlayerBoard);
                gameState.setComputerBoard(emptyOpponentBoard);
            }

            // Сохраняем в кэш с учетом userId
            gameStateCache.put(cacheKey, gameState);
            lastUpdateTime.put(cacheKey, System.currentTimeMillis());
            
            return gameState;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации/десериализации комнаты", e);
        }
    }

    // Скрывает корабли на доске: оставляет только попадания (3) и промахи (2), все 1 превращает в 0
    private int[][] hideShipsOnBoard(int[][] board) {
        int[][] result = new int[10][10];
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[y][x] == 2 || board[y][x] == 3) {
                    result[y][x] = board[y][x];
                } else {
                    result[y][x] = 0;
                }
            }
        }
        return result;
    }
    
    private boolean checkShipSunk(List<ShipDto> ships, int[][] board, int x, int y) {
        for (ShipDto ship : ships) {
            List<int[]> positions = ship.getPositions();
            for (int i = 0; i < positions.size(); i++) {
                int[] pos = positions.get(i);
                if (pos[0] == x && pos[1] == y) {
                    // Проверяем все клетки корабля
                    boolean allHit = true;
                    for (int[] shipPos : positions) {
                        if (board[shipPos[1]][shipPos[0]] != 3) {
                            allHit = false;
                            break;
                        }
                    }
                    return allHit;
                }
            }
        }
        return false;
    }

    private boolean checkGameOver(int[][] board) {
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (board[y][x] == 1) { // Если остались неподбитые корабли
                    return false;
                }
            }
        }
        return true;
    }
} // конец класса GameService