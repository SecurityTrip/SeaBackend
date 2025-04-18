package ru.securitytrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipDto {
    private int size;
    private int x;
    private int y;
    private boolean isHorizontal;
    private boolean[] hits; // null для создания корабля, заполнено для существующего
} 