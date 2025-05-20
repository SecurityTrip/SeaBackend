package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Setter
@Getter
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
    
    @Schema(description = "Список координат клеток корабля (каждый элемент — [x, y])", example = "[[0,0],[0,1],[0,2]]", nullable = true)
    private java.util.List<int[]> positions;

    @Schema(description = "Список попаданий по кораблю (каждый элемент — [x, y] — координата попадания)", example = "[[0,1],[0,2]]", nullable = true)
    private java.util.List<int[]> hitsList;
    
    public boolean isHorizontal() {
        return isHorizontal;
    }
    
    public void setHorizontal(boolean horizontal) {
        isHorizontal = horizontal;
    }
}