package ru.securitytrip.backend;

import org.junit.jupiter.api.Test;
import ru.securitytrip.backend.model.GameBoard;
import ru.securitytrip.backend.model.Ship;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameBoardTest {

    @Test
    public void testInitEmptyBoard() {
        GameBoard board = new GameBoard();
        board.initEmptyBoard();
        
        int[][] boardArray = board.getBoardAsArray();
        
        // Проверяем, что доска имеет размеры 10x10
        assertEquals(10, boardArray.length);
        assertEquals(10, boardArray[0].length);
        
        // Проверяем, что все ячейки пустые (0)
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(0, boardArray[i][j]);
            }
        }
    }
    
    @Test
    public void testConvertBoardToString() {
        GameBoard board = new GameBoard();
        
        // Создаем тестовую доску 3x3
        int[][] testBoard = {
            {0, 1, 0},
            {0, 1, 0},
            {0, 0, 0}
        };
        
        String boardString = board.convertBoardToString(testBoard);
        
        // Ожидаемый формат: "0,1,0;0,1,0;0,0,0"
        assertEquals("0,1,0;0,1,0;0,0,0", boardString);
    }
    
    @Test
    public void testGetBoardAsArray() {
        GameBoard board = new GameBoard();
        
        // Устанавливаем строковое представление доски
        String boardString = "0,1,0;0,1,0;0,0,0";
        board.setBoardState(boardString);
        
        int[][] boardArray = board.getBoardAsArray();
        
        // Проверяем размеры
        assertEquals(3, boardArray.length);
        assertEquals(3, boardArray[0].length);
        
        // Проверяем содержимое
        assertEquals(0, boardArray[0][0]);
        assertEquals(1, boardArray[0][1]);
        assertEquals(0, boardArray[0][2]);
        assertEquals(0, boardArray[1][0]);
        assertEquals(1, boardArray[1][1]);
        assertEquals(0, boardArray[1][2]);
        assertEquals(0, boardArray[2][0]);
        assertEquals(0, boardArray[2][1]);
        assertEquals(0, boardArray[2][2]);
    }
    
    @Test
    public void testShipCoordinates() {
        // Создаем горизонтальный корабль размером 3 в координатах (1,2)
        Ship ship = new Ship(3, 1, 2, true);
        
        List<int[]> coordinates = ship.getCoordinates();
        
        // Проверяем, что список содержит 3 координаты
        assertEquals(3, coordinates.size());
        
        // Проверяем, что координаты соответствуют ожидаемым
        assertArrayEquals(new int[]{1, 2}, coordinates.get(0));
        assertArrayEquals(new int[]{2, 2}, coordinates.get(1));
        assertArrayEquals(new int[]{3, 2}, coordinates.get(2));
        
        // Создаем вертикальный корабль размером 2 в координатах (4,3)
        Ship verticalShip = new Ship(2, 4, 3, false);
        
        List<int[]> verticalCoordinates = verticalShip.getCoordinates();
        
        // Проверяем, что список содержит 2 координаты
        assertEquals(2, verticalCoordinates.size());
        
        // Проверяем, что координаты соответствуют ожидаемым
        assertArrayEquals(new int[]{4, 3}, verticalCoordinates.get(0));
        assertArrayEquals(new int[]{4, 4}, verticalCoordinates.get(1));
    }
    
    @Test
    public void testShipHits() {
        // Создаем горизонтальный корабль размером 3 в координатах (1,2)
        Ship ship = new Ship(3, 1, 2, true);
        
        // Попытки попадания
        assertFalse(ship.hit(0, 2)); // Мимо (слева)
        assertTrue(ship.hit(1, 2));  // Попадание в первую клетку
        assertTrue(ship.hit(2, 2));  // Попадание во вторую клетку
        assertFalse(ship.hit(1, 3)); // Мимо (сверху)
        
        // Проверяем статус потопления
        assertFalse(ship.isSunk()); // Еще не потоплен (не все клетки поражены)
        
        // Поражаем последнюю клетку
        assertTrue(ship.hit(3, 2));
        
        // Проверяем, что корабль потоплен
        assertTrue(ship.isSunk());
    }
} 