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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(GameService.class);
    
    @Autowired
    private GameRepository gameRepository;
    
    @Autowired
    private GameBoardRepository gameBoardRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final Random random = new Random();
    
    // Стандартные размеры кораблей для морского боя
    private static final int[] STANDARD_SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};
    
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
        
        while (!placed && attempts < 100) {
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
            throw new RuntimeException("Не удалось разместить корабль размером " + shipSize);
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
                    // Если клетка занята, нельзя ставить
                    if (board[checkY][checkX] != 0) {
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
        
        for (GameBoard board : game.getBoards()) {
            GameBoardDto boardDto = new GameBoardDto();
            boardDto.setId(board.getId());
            boardDto.setBoard(board.getBoardAsArray());
            boardDto.setComputer(board.isComputer());
            
            try {
                // Для игрока показываем все корабли, для компьютера - только подбитые части
                if (userId != null && userId.equals(board.getOwnerId())) {
                    List<Ship> ships = objectMapper.readValue(board.getShipsData(), 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
                    
                    boardDto.setShips(ships.stream().map(this::convertToShipDto).collect(Collectors.toList()));
                    dto.setPlayerBoard(boardDto);
                } else {
                    // Для компьютера показываем только подбитые корабли
                    if (game.getGameState() == GameState.PLAYER_WON || game.getGameState() == GameState.COMPUTER_WON) {
                        // После окончания игры показываем все корабли компьютера
                        List<Ship> ships = objectMapper.readValue(board.getShipsData(), 
                                objectMapper.getTypeFactory().constructCollectionType(List.class, Ship.class));
                        
                        boardDto.setShips(ships.stream().map(this::convertToShipDto).collect(Collectors.toList()));
                    } else {
                        // Во время игры не показываем расположение кораблей компьютера
                        boardDto.setShips(new ArrayList<>());
                    }
                    dto.setComputerBoard(boardDto);
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
        
        // Преобразуем List<Boolean> в boolean[]
        List<Boolean> hitsList = ship.getHits();
        boolean[] hitsArray = new boolean[hitsList.size()];
        for (int i = 0; i < hitsList.size(); i++) {
            hitsArray[i] = hitsList.get(i);
        }
        dto.setHits(hitsArray);
        
        return dto;
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
        } else {
            // Промах (2)
            boardArray[moveRequest.getY()][moveRequest.getX()] = 2;
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
        } else if (!hit) {
            // Если промах, переключаем ход на компьютер
            game.toggleTurn();
        }
        
        // Сохраняем изменения
        game = gameRepository.save(game);
        
        // Формируем ответ
        MoveResponse response = new MoveResponse(hit, sunk, gameOver);
        
        // Если игра не закончилась и был промах, делаем ход компьютера
        if (!gameOver && !hit) {
            computerMove(game);
        }
        
        // Обновляем состояние игры для ответа
        response.setGameState(convertToGameDto(game, userId));
        
        return response;
    }
    
    @Transactional
    public void computerMove(Game game) {
        // Получаем доску игрока
        GameBoard playerBoard = game.getPlayerBoard();
        
        // Получаем текущее состояние доски
        int[][] boardArray = playerBoard.getBoardAsArray();
        
        // Получаем уровень сложности
        DifficultyLevel difficultyLevel = game.getDifficultyLevel();
        
        // Координаты выстрела
        int x = 0, y = 0;
        boolean validMove = false;
        
        // Выбираем стратегию в зависимости от сложности
        switch (difficultyLevel) {
            case EASY:
                // Полностью случайные выстрелы
                validMove = makeRandomShot(boardArray, x, y);
                break;
                
            case MEDIUM:
                // Выстрелы по шахматной доске
                validMove = makeCheckerboardShot(boardArray, x, y);
                break;
                
            case HARD:
                // Случайные выстрелы с добиванием кораблей
                validMove = makeSmartShot(boardArray, x, y, game);
                break;
                
            default:
                // По умолчанию - случайный выстрел
                validMove = makeRandomShot(boardArray, x, y);
        }
        
        if (validMove) {
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
            } else {
                // Промах (2)
                boardArray[y][x] = 2;
            }
            
            // Обновляем доску
            playerBoard.setBoardState(playerBoard.convertBoardToString(boardArray));
            
            // Если был HARD уровень и попадание, сохраняем последний удачный выстрел
            if (hit && difficultyLevel == DifficultyLevel.HARD) {
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
                
                // Если игра закончилась, обновляем статус
                if (gameOver) {
                    game.setGameState(GameState.COMPUTER_WON);
                } else {
                    // Если попал, компьютер ходит еще раз (рекурсивно)
                    gameRepository.save(game);
                    computerMove(game);
                    return;
                }
            } else {
                // Сбрасываем последний удачный выстрел, если это был промах на HARD
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
    private boolean makeRandomShot(int[][] boardArray, int x, int y) {
        boolean validMove = false;
        while (!validMove) {
            x = random.nextInt(10);
            y = random.nextInt(10);
            
            // Проверяем, что по этой клетке еще не стреляли
            if (boardArray[y][x] != 2 && boardArray[y][x] != 3) {
                validMove = true;
            }
        }
        return validMove;
    }
    
    // Метод для выстрела по шахматной доске (для MEDIUM)
    private boolean makeCheckerboardShot(int[][] boardArray, int x, int y) {
        boolean validMove = false;
        
        // Попытки найти ход по шахматному паттерну
        for (int attempts = 0; attempts < 100 && !validMove; attempts++) {
            x = random.nextInt(10);
            y = random.nextInt(10);
            
            // Проверяем, что клетка следует шахматному паттерну
            // (сумма координат четная), и по ней еще не стреляли
            if ((x + y) % 2 == 0 && boardArray[y][x] != 2 && boardArray[y][x] != 3) {
                validMove = true;
            }
        }
        
        // Если не удалось найти ход по шахматному паттерну, делаем случайный ход
        if (!validMove) {
            return makeRandomShot(boardArray, x, y);
        }
        
        return validMove;
    }
    
    // Метод для умного выстрела с добиванием кораблей (для HARD)
    private boolean makeSmartShot(int[][] boardArray, int x, int y, Game game) {
        boolean validMove = false;
        
        // Пытаемся атаковать вокруг последнего попадания, если оно было
        if (game.getLastHitX() >= 0 && game.getLastHitY() >= 0) {
            // Направления для проверки (вверх, вправо, вниз, влево)
            int[][] directions = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
            
            for (int[] dir : directions) {
                int newX = game.getLastHitX() + dir[0];
                int newY = game.getLastHitY() + dir[1];
                
                // Проверяем, что координаты в пределах поля и клетка не простреляна
                if (newX >= 0 && newX < 10 && newY >= 0 && newY < 10 && 
                        boardArray[newY][newX] != 2 && boardArray[newY][newX] != 3) {
                    x = newX;
                    y = newY;
                    validMove = true;
                    break;
                }
            }
        }
        
        // Если не удалось найти умный ход, делаем случайный
        if (!validMove) {
            return makeRandomShot(boardArray, x, y);
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
        
        // Проверяем, что пользователь имеет доступ к игре (является владельцем доски игрока)
        GameBoard playerBoard = game.getPlayerBoard();
        if (playerBoard == null || !userId.equals(playerBoard.getOwnerId())) {
            throw new RuntimeException("У вас нет доступа к этой игре");
        }
        
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
} 