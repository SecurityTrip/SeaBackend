package ru.securitytrip.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.securitytrip.backend.dto.LoginRequest;
import ru.securitytrip.backend.dto.LoginResponse;
import ru.securitytrip.backend.dto.RegisterRequest;
import ru.securitytrip.backend.dto.RegisterResponse;
import ru.securitytrip.backend.service.AuthService;

@RestController
@RequestMapping("/auth")
@Tag(name = "Аутентификация", description = "API для аутентификации и регистрации пользователей")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Operation(summary = "Вход в систему", description = "Аутентификация пользователя и получение JWT-токена")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная аутентификация",
                    content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные", 
                    content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
        logger.info("Получен запрос на аутентификацию для пользователя: {}", loginRequest.getUsername());
        try {
            LoginResponse response = authService.authenticateUser(loginRequest);
            logger.info("Успешная аутентификация пользователя: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Ошибка при аутентификации пользователя {}: {}", loginRequest.getUsername(), e.getMessage());
            throw e;
        }
    }

    @Operation(summary = "Регистрация нового пользователя", description = "Создание нового аккаунта пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная регистрация",
                    content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации или пользователь уже существует", 
                    content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest registerRequest) {
        logger.info("Получен запрос на регистрацию пользователя: {}", registerRequest.getUsername());
        RegisterResponse response = authService.registerUser(registerRequest);
        
        if (response.isSuccess()) {
            logger.info("Пользователь успешно зарегистрирован: {}", registerRequest.getUsername());
        } else {
            logger.warn("Не удалось зарегистрировать пользователя {}: {}", 
                    registerRequest.getUsername(), response.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Обновление access-токена по refresh-токену", description = "Позволяет получить новый access-токен и refresh-токен по действующему refresh-токену.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен успешно обновлён",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh токен невалиден или истёк",
                    content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestBody String refreshToken) {
        LoginResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
