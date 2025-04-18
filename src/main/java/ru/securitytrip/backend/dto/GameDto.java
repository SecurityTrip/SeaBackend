package ru.securitytrip.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.securitytrip.backend.model.GameMode;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameDto {
    private Long id;
    private GameMode mode;
    private GameBoardDto playerBoard;
    private GameBoardDto computerBoard;
}

