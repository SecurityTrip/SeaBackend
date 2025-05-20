package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import ru.securitytrip.backend.model.GameMode;
import ru.securitytrip.backend.model.GameState;

import java.util.List;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о текущем состоянии игры")
public class GameDto {
    @Schema(description = "Идентификатор игры", example = "1")
    private Long id;
    
    @Schema(description = "Режим игры (SINGLEPLAYER/MULTIPLAYER)", example = "SINGLEPLAYER")
    private GameMode mode;
    
    @Schema(description = "Текущее состояние игры", example = "IN_PROGRESS", 
            allowableValues = {"WAITING", "IN_PROGRESS", "PLAYER_WON", "COMPUTER_WON"})
    private GameState gameState;
    
    @Schema(description = "Флаг, показывающий, чей сейчас ход (true - ход игрока, false - ход компьютера)", 
            example = "true")
    private boolean playerTurn;
    
    @Schema(description = "Информация о доске игрока")
    private GameBoardDto playerBoard;
    
    @Schema(description = "Информация о доске компьютера")
    private GameBoardDto computerBoard;

    public void setPlayerBoard(GameBoardDto playerBoard) {
        this.playerBoard = playerBoard;
    }
    public void setComputerBoard(GameBoardDto computerBoard) {
        this.computerBoard = computerBoard;
    }
    public void setPlayerTurn(boolean playerTurn) {
        this.playerTurn = playerTurn;
    }
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
    public void setMode(GameMode mode) {
        this.mode = mode;
    }
}

