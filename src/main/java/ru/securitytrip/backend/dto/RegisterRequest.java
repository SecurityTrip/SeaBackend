package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на регистрацию нового пользователя")
public class RegisterRequest {
    @Schema(description = "Имя пользователя", example = "new_user123", required = true)
    private String username;
    
    @Schema(description = "Пароль пользователя", example = "securePassword123", required = true)
    private String password;
    
    @Schema(description = "Идентификатор аватара", example = "1", required = true)
    private int avatarId;
}
