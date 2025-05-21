package ru.securitytrip.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.securitytrip.backend.dto.GameDto;
import ru.securitytrip.backend.dto.ShipDto;
import ru.securitytrip.backend.repository.MultiplayerRoomRepository;
import ru.securitytrip.backend.model.MultiplayerRoomEntity;
import ru.securitytrip.backend.model.GameMode;
import ru.securitytrip.backend.model.GameState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class MultiplayerGameServiceTest {
    private GameService gameService;
    private ObjectMapper objectMapper;

    @Mock
    private MultiplayerRoomRepository multiplayerRoomRepository;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        gameService = new GameService();
        objectMapper = new ObjectMapper();
        
        Field objectMapperField = GameService.class.getDeclaredField("objectMapper");
        objectMapperField.setAccessible(true);
        objectMapperField.set(gameService, objectMapper);

        Field repositoryField = GameService.class.getDeclaredField("multiplayerRoomRepository");
        repositoryField.setAccessible(true);
        repositoryField.set(gameService, multiplayerRoomRepository);

        when(multiplayerRoomRepository.save(any())).thenAnswer(i -> {
            MultiplayerRoomEntity room = (MultiplayerRoomEntity) i.getArgument(0);
            when(multiplayerRoomRepository.findById(room.getCode())).thenReturn(Optional.of(room));
            return room;
        });
    }

    @Test
    void testMultiplayerGameFullCycle() throws Exception {
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
        
        Field gameStateField = GameDto.class.getDeclaredField("gameState");
        gameStateField.setAccessible(true);
        assertNotNull(gameStateField.get(state));
    }

    private List<ShipDto> createTestShips() throws Exception {
        List<ShipDto> ships = new ArrayList<>();
        ShipDto ship = new ShipDto();
        
        Field sizeField = ShipDto.class.getDeclaredField("size");
        sizeField.setAccessible(true);
        sizeField.set(ship, 2);
        
        Field xField = ShipDto.class.getDeclaredField("x");
        xField.setAccessible(true);
        xField.set(ship, 0);
        
        Field yField = ShipDto.class.getDeclaredField("y");
        yField.setAccessible(true);
        yField.set(ship, 0);
        
        Field horizontalField = ShipDto.class.getDeclaredField("isHorizontal");
        horizontalField.setAccessible(true);
        horizontalField.set(ship, true);
        
        List<int[]> positions = new ArrayList<>();
        positions.add(new int[]{0, 0});
        positions.add(new int[]{1, 0});
        
        Field positionsField = ShipDto.class.getDeclaredField("positions");
        positionsField.setAccessible(true);
        positionsField.set(ship, positions);
        
        ships.add(ship);
        return ships;
    }
}
