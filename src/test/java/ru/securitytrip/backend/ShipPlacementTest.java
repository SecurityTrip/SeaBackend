package ru.securitytrip.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.securitytrip.backend.model.Ship;
import ru.securitytrip.backend.service.GameService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ShipPlacementTest {

    @Autowired
    private GameService gameService;

    private static final int BOARD_SIZE = 10;
    private static final int[] STANDARD_SHIP_SIZES = {4, 3, 3, 2, 2, 2, 1, 1, 1, 1};

    @Test
    public void testShoreStrategyPlacement() {
        List<Ship> ships = convertToShips(gameService.generatePlayerShips("SHORE"));
        validateShipPlacement(ships);
    }

    @Test
    public void testAsymmetricStrategyPlacement() {
        List<Ship> ships = convertToShips(gameService.generatePlayerShips("ASYMMETRIC"));
        validateShipPlacement(ships);
    }

    @Test
    public void testRandomStrategyPlacement() {
        List<Ship> ships = convertToShips(gameService.generatePlayerShips("RANDOM"));
        validateShipPlacement(ships);
    }

    private List<Ship> convertToShips(List<ru.securitytrip.backend.dto.ShipDto> shipDtos) {
        return shipDtos.stream()
            .map(dto -> new Ship(dto.getSize(), dto.getX(), dto.getY(), dto.isHorizontal()))
            .collect(Collectors.toList());
    }

    private void validateShipPlacement(List<Ship> ships) {
        // Проверяем общее количество кораблей
        assertEquals(10, ships.size(), "Должно быть ровно 10 кораблей");

        // Проверяем количество кораблей каждого размера
        Map<Integer, Long> sizeCount = ships.stream()
                .collect(Collectors.groupingBy(ship -> ship.getSize(), Collectors.counting()));
        
        assertEquals(1, sizeCount.get(4), "Должен быть 1 корабль размером 4");
        assertEquals(2, sizeCount.get(3), "Должно быть 2 корабля размером 3");
        assertEquals(3, sizeCount.get(2), "Должно быть 3 корабля размером 2");
        assertEquals(4, sizeCount.get(1), "Должно быть 4 корабля размером 1");

        // Проверяем, что корабли не выходят за пределы доски
        for (Ship ship : ships) {
            List<int[]> coordinates = ship.getCoordinates();
            for (int[] coord : coordinates) {
                assertTrue(coord[0] >= 0 && coord[0] < BOARD_SIZE, 
                    "X координата должна быть в пределах [0, " + (BOARD_SIZE-1) + "]");
                assertTrue(coord[1] >= 0 && coord[1] < BOARD_SIZE, 
                    "Y координата должна быть в пределах [0, " + (BOARD_SIZE-1) + "]");
            }
        }

        // Проверяем, что корабли не пересекаются и не соприкасаются
        for (int i = 0; i < ships.size(); i++) {
            for (int j = i + 1; j < ships.size(); j++) {
                assertFalse(shipsOverlap(ships.get(i), ships.get(j)), 
                    "Корабли не должны пересекаться или соприкасаться");
            }
        }
    }

    private boolean shipsOverlap(Ship ship1, Ship ship2) {
        List<int[]> coords1 = ship1.getCoordinates();
        List<int[]> coords2 = ship2.getCoordinates();

        // Проверяем каждую клетку первого корабля
        for (int[] coord1 : coords1) {
            // Проверяем каждую клетку второго корабля
            for (int[] coord2 : coords2) {
                // Если клетки совпадают или находятся рядом
                if (Math.abs(coord1[0] - coord2[0]) <= 1 && 
                    Math.abs(coord1[1] - coord2[1]) <= 1) {
                    return true;
                }
            }
        }
        return false;
    }
} 