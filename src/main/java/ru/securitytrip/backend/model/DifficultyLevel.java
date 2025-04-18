package ru.securitytrip.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Уровни сложности игры")
public enum DifficultyLevel {
    @Schema(description = "Легкий уровень - случайные выстрелы компьютера, расстановка по стратегии берега")
    EASY,
    
    @Schema(description = "Средний уровень - выстрелы по шахматной доске, ассиметричная расстановка")
    MEDIUM,
    
    @Schema(description = "Сложный уровень - случайные выстрелы, но прицельное добивание кораблей")
    HARD
} 