package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на размещение кораблей хоста в мультиплеерной игре")
public class PlaceHostShipsRequest {
    @Schema(description = "Код комнаты (игры)")
    private String gameCode;
    @Schema(description = "Расстановка кораблей хоста")
    private List<ShipDto> ships;
    @Schema(description = "ID пользователя (уникальный для каждого клиента)")
    private Long userId;

    public String getGameCode() { return gameCode; }
    public List<ShipDto> getShips() { return ships; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
