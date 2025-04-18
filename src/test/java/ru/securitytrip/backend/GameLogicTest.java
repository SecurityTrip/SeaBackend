package ru.securitytrip.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.securitytrip.backend.model.GameBoard;
import ru.securitytrip.backend.model.Ship;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameLogicTest {
    
    private GameBoard board;
    private List<Ship> ships;
    
    @BeforeEach
    public void setup() {
        board = new GameBoard();
        board.initEmptyBoard();
        
        ships = new ArrayList<>();
        // Добавляем три корабля: 1x3, 1x2, 1x1
        ships.add(new Ship(3, 0, 0, true));  // Горизонтальный корабль в левом верхнем углу
        ships.add(new Ship(2, 5, 5, false)); // Вертикальный корабль в центре
        ships.add(new Ship(1, 9, 9, true));  // Одиночная клетка в правом нижнем углу
        
        // Размещаем корабли на доске
        int[][] boardArray = board.getBoardAsArray();
        for (Ship ship : ships) {
            List<int[]> coordinates = ship.getCoordinates();
            for (int[] coord : coordinates) {
                boardArray[coord[1]][coord[0]] = 1; // Отмечаем клетки кораблей
            }
        }
        
        // Обновляем состояние доски
        board.setBoardState(board.convertBoardToString(boardArray));
    }
    
    @Test
    public void testHitMiss() {
        // Промах (пустая клетка)
        boolean hit = false;
        for (Ship ship : ships) {
            if (ship.hit(3, 3)) {
                hit = true;
                break;
            }
        }
        assertFalse(hit);
        
        // Попадание в первый корабль
        hit = false;
        for (Ship ship : ships) {
            if (ship.hit(1, 0)) {
                hit = true;
                break;
            }
        }
        assertTrue(hit);
    }
    
    @Test
    public void testSinkShip() {
        // Потопим корабль размером 1x1
        Ship smallShip = ships.get(2);
        
        // Проверяем, что корабль изначально не потоплен
        assertFalse(smallShip.isSunk());
        
        // Делаем выстрел
        assertTrue(smallShip.hit(9, 9));
        
        // Проверяем, что корабль потоплен
        assertTrue(smallShip.isSunk());
    }
    
    @Test
    public void testMultipleHitsToSink() {
        // Потопим корабль размером 1x3
        Ship largeShip = ships.get(0);
        
        // Проверяем, что корабль изначально не потоплен
        assertFalse(largeShip.isSunk());
        
        // Делаем выстрелы
        assertTrue(largeShip.hit(0, 0));
        assertFalse(largeShip.isSunk()); // После первого попадания не потоплен
        
        assertTrue(largeShip.hit(1, 0));
        assertFalse(largeShip.isSunk()); // После второго попадания не потоплен
        
        assertTrue(largeShip.hit(2, 0));
        assertTrue(largeShip.isSunk()); // После третьего попадания потоплен
    }
} 