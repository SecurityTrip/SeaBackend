package ru.securitytrip.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.securitytrip.backend.dto.UpdateUserRequest;
import ru.securitytrip.backend.service.UserService;

@RestController
@RequestMapping("/user")
@Tag(name = "Пользователь", description = "API для управления профилем пользователя")
public class UserController {
    @Autowired
    private UserService userService;

    @Operation(summary = "Обновить профиль пользователя", description = "Позволяет изменить username, пароль и avatarId текущего пользователя.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Профиль успешно обновлён"),
            @ApiResponse(responseCode = "400", description = "Некорректные данные"),
            @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PutMapping("/me")
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Long userId = userService.findByUsername(username).getId();
        userService.updateUser(userId, request);
        return ResponseEntity.ok().build();
    }
}
