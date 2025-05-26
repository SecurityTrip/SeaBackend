package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на создание мультиплеерной игры")
public class CreateMultiplayerGameRequest {
    @Schema(description = "Расстановка кораблей игрока")
    private java.util.List<ShipDto> ships;

    @Schema(description = "userId игрока (генерируется на фронте и передаётся явно)")
    private Long userId;

    public java.util.List<ShipDto> getShips() { return ships; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
