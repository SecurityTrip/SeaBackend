package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Информация о корабле")
public class ShipDto {
    @Schema(description = "Размер корабля (количество клеток)", example = "4", minimum = "1", maximum = "4", required = true)
    private int size;
    
    @Schema(description = "Координата X начальной точки корабля", example = "0", minimum = "0", maximum = "9", required = true)
    private int x;
    
    @Schema(description = "Координата Y начальной точки корабля", example = "0", minimum = "0", maximum = "9", required = true)
    private int y;
    
    @Schema(description = "Ориентация корабля (true - горизонтальная, false - вертикальная)", example = "true", required = true)
    private boolean isHorizontal;
    
    @Schema(description = "Массив попаданий по клеткам корабля (true - клетка поражена)",
            example = "[false, true, false, false]",
            nullable = true)
    private boolean[] hits; // null для создания корабля, заполнено для существующего
} 