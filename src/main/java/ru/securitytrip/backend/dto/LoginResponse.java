package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ на успешную аутентификацию")
public class LoginResponse {
    @Schema(description = "JWT токен для авторизации", example = "eyJhbGciOiJIUzI1NiJ9...", required = true)
    private String token;
    
    @Schema(description = "Имя пользователя", example = "user123", required = true)
    private String username;
    
    @Schema(description = "Идентификатор пользователя", example = "1", required = true)
    private Long userId;

    @Schema(description = "Идентификатор аватара пользователя", example = "1", required = true)
    private int avatarId;

    @Schema(description = "Refresh токен для обновления доступа", example = "eyJhbGciOiJIUzI1NiJ9...", required = true)
    private String refreshToken;
}
