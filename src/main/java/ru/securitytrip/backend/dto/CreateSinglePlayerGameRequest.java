package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;
import ru.securitytrip.backend.model.DifficultyLevel;

import java.util.List;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на создание одиночной игры с указанной расстановкой кораблей")
public class CreateSinglePlayerGameRequest {
    @Schema(description = "Список кораблей с их координатами и ориентацией", 
            example = "[{\"size\":4,\"x\":0,\"y\":0,\"horizontal\":true}]",
            required = true)
    private List<ShipDto> ships;
    
    @Schema(description = "Уровень сложности игры", 
            example = "MEDIUM", 
            allowableValues = {"EASY", "MEDIUM", "HARD"},
            defaultValue = "MEDIUM")
    private DifficultyLevel difficultyLevel = DifficultyLevel.MEDIUM;

}