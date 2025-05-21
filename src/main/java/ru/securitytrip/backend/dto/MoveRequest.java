package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на выполнение хода в игре")
public class MoveRequest {
    @Schema(description = "Идентификатор игры", example = "1", required = true)
    private Long gameId;
    
    @Schema(description = "Координата X клетки, по которой производится выстрел", 
            example = "5", minimum = "0", maximum = "9", required = true)
    private int x;
    
    @Schema(description = "Координата Y клетки, по которой производится выстрел", 
            example = "3", minimum = "0", maximum = "9", required = true)
    private int y;

    @Schema(description = "Код комнаты (gameCode) для мультиплеерной игры", example = "ABC123", required = false)
    private String gameCode;
    public String getGameCode() { return gameCode; }
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }
}