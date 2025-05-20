package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ с кодом комнаты для подключения к мультиплеерной игре")
public class MultiplayerGameCodeResponse {
    @Schema(description = "Код комнаты (игры)")
    private String gameCode;
}
