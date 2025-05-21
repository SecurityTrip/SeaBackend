package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на подключение к мультиплеерной игре по коду")
public class JoinMultiplayerGameRequest {
    @Schema(description = "Код комнаты (игры)")
    private String gameCode;
    @Schema(description = "Расстановка кораблей игрока")
    private java.util.List<ShipDto> ships;

    public String getGameCode() { return gameCode; }
    public java.util.List<ShipDto> getShips() { return ships; }
}
