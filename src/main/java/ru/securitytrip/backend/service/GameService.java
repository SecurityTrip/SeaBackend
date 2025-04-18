package ru.securitytrip.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.securitytrip.backend.dto.CreateSinglePlayerGameRequest;
import ru.securitytrip.backend.dto.GameBoardDto;
import ru.securitytrip.backend.dto.GameDto;
import ru.securitytrip.backend.dto.ShipDto;
import ru.securitytrip.backend.model.Game;
import ru.securitytrip.backend.model.GameBoard;
import ru.securitytrip.backend.model.GameMode;
import ru.securitytrip.backend.model.Ship;
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
        
        // Генерируем случайную расстановку кораблей для компьютера
        List<Ship> computerShips = generateRandomShips();
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
    
    // Метод для генерации случайной расстановки кораблей
    private List<Ship> generateRandomShips() {
        List<Ship> ships = new ArrayList<>();
        int[][] board = new int[10][10]; // 0 - пусто, 1 - корабль, 2 - зона вокруг корабля
        
        // Для каждого стандартного размера корабля
        for (int shipSize : STANDARD_SHIP_SIZES) {
            boolean placed = false;
            int attempts = 0;
            
            // Пытаемся разместить корабль до успеха или превышения максимального числа попыток
            while (!placed && attempts < 100) {
                int x = random.nextInt(10);
                int y = random.nextInt(10);
                boolean horizontal = random.nextBoolean();
                
                // Проверяем, можно ли разместить корабль в этой позиции
                if (canPlaceShip(board, x, y, shipSize, horizontal)) {
                    // Размещаем корабль на доске
                    placeShipOnBoard(board, x, y, shipSize, horizontal);
                    ships.add(new Ship(shipSize, x, y, horizontal));
                    placed = true;
                }
                
                attempts++;
            }
            
            // Если не удалось разместить корабль, генерируем исключение
            if (!placed) {
                throw new RuntimeException("Не удалось сгенерировать случайную расстановку кораблей");
            }
        }
        
        return ships;
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
                    // Для компьютера не показываем положение кораблей
                    boardDto.setShips(new ArrayList<>());
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
} 