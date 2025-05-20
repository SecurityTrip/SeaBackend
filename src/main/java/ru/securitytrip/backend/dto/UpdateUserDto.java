package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UpdateUserDto {
    @Schema(description = "Имя пользователя", example = "user123")
    private String username;

    @Schema(description = "Зашифрованный пароль пользователя", example = "$2a$10$X7L...")
    private String password;

    @Schema(description = "Идентификатор аватара пользователя", example = "1")
    private int avatarId;
}
