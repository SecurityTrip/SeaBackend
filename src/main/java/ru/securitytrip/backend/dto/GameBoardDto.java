package ru.securitytrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameBoardDto {
    private Long id;
    private int[][] board;
    private List<ShipDto> ships;
    private boolean isComputer;
} 