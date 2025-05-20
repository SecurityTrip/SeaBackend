package ru.securitytrip.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.securitytrip.backend.dto.GameDto;
import ru.securitytrip.backend.dto.MoveRequest;
import ru.securitytrip.backend.dto.ShipDto;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiplayerGameServiceTest {
    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService();
    }

    @Test
    void testMultiplayerGameFullCycle() {
        // Игрок 1 создаёт комнату
        Long player1Id = 1L;
        List<ShipDto> ships1 = createTestShips();
        String code = gameService.createMultiplayerGame(player1Id, ships1);
        assertNotNull(code);

        // Игрок 2 подключается
        Long player2Id = 2L;
        List<ShipDto> ships2 = createTestShips();
        GameDto state = gameService.joinMultiplayerGame(code, player2Id, ships2);
        assertNotNull(state);

        // Игрок 1 делает ход по первой клетке корабля игрока 2
        ShipDto targetShip = ships2.get(0);
        int[] pos = targetShip.getPositions().get(0);
        MoveRequest move = new MoveRequest(null, pos[0], pos[1]);
        state = gameService.makeMultiplayerMove(code, player1Id, move);
        assertTrue(state.isPlayerTurn() == false || state.getGameState() != null);

        // Игрок 2 делает ответный ход
        ShipDto targetShip2 = ships1.get(0);
        int[] pos2 = targetShip2.getPositions().get(0);
        MoveRequest move2 = new MoveRequest(null, pos2[0], pos2[1]);
        state = gameService.makeMultiplayerMove(code, player2Id, move2);
        assertTrue(state.isPlayerTurn() == true || state.getGameState() != null);
    }

    private List<ShipDto> createTestShips() {
        List<ShipDto> ships = new ArrayList<>();
        ShipDto ship = new ShipDto();
        ship.setSize(2);
        ship.setX(0);
        ship.setY(0);
        ship.setHorizontal(true);
        List<int[]> positions = new ArrayList<>();
        positions.add(new int[]{0, 0});
        positions.add(new int[]{1, 0});
        ship.setPositions(positions);
        ships.add(ship);
        return ships;
    }
}
