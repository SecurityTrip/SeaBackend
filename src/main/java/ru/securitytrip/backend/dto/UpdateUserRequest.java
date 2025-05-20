package ru.securitytrip.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на обновление профиля пользователя")
public class UpdateUserRequest {
    @Schema(description = "Новый username", example = "new_username")
    private String username;

    @Schema(description = "Новый пароль", example = "new_password")
    private String password;

    @Schema(description = "Новый avatarId", example = "2")
    private Integer avatarId;
}
