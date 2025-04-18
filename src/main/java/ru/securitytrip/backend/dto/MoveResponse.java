package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ на запрос хода с результатами и обновленным состоянием игры")
public class MoveResponse {
    @Schema(description = "Флаг попадания (true - попадание, false - промах)", example = "true")
    private boolean hit;        // Было ли попадание
    
    @Schema(description = "Флаг потопления корабля (true - корабль потоплен)", example = "false")
    private boolean sunk;       // Был ли потоплен корабль
    
    @Schema(description = "Флаг завершения игры (true - игра окончена)", example = "false")
    private boolean gameOver;   // Закончилась ли игра
    
    @Schema(description = "Обновленное состояние игры после хода")
    private GameDto gameState;  // Текущее состояние игры
    
    // Конструктор для быстрого создания ответа
    public MoveResponse(boolean hit, boolean sunk, boolean gameOver) {
        this.hit = hit;
        this.sunk = sunk;
        this.gameOver = gameOver;
    }

}