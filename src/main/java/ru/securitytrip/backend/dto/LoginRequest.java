package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос для аутентификации пользователя")
public class LoginRequest {
    @Schema(description = "Имя пользователя", example = "user123", required = true)
    private String username;
    
    @Schema(description = "Пароль пользователя", example = "password123", required = true)
    private String password;
}
