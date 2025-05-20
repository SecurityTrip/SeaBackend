package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Schema(description = "Информация об игровой доске")
public class GameBoardDto {
    @Schema(description = "Идентификатор доски", example = "1")
    private Long id;

    @Schema(description = "Двумерный массив с состоянием клеток доски (0 - пусто, 1 - корабль, 2 - промах, 3 - попадание)",
            example = "[[0,0,0],[1,0,0],[0,3,2]]")
    private int[][] board;

    @Schema(description = "Список кораблей на доске")
    private List<ShipDto> ships;
    
    @Schema(description = "Флаг, указывающий, принадлежит ли доска компьютеру", example = "false")
    private boolean isComputer;

    public boolean isComputer() {
        return isComputer;
    }

    public void setBoard(int[][] board) {
        this.board = board;
    }
    public void setShips(java.util.List<ShipDto> ships) {
        this.ships = ships;
    }
    public void setComputer(boolean isComputer) {
        this.isComputer = isComputer;
    }
}