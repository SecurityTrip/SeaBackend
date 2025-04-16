package ru.securitytrip.backend.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Сущность пользователя")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Schema(description = "Уникальный идентификатор пользователя", example = "1")
    private Long id;

    @Schema(description = "Имя пользователя", example = "user123")
    private String username;
    
    @Schema(description = "Зашифрованный пароль пользователя", example = "$2a$10$X7L...")
    private String password;
    
    @Schema(description = "Идентификатор аватара пользователя", example = "1")
    private int avatarId;
}
