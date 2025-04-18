package ru.securitytrip.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.securitytrip.backend.dto.CreateSinglePlayerGameRequest;
import ru.securitytrip.backend.dto.GameDto;
import ru.securitytrip.backend.service.GameService;

@RestController
@RequestMapping("/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/singleplayer")
    public ResponseEntity<GameDto> createSinglePlayerGame(@RequestBody CreateSinglePlayerGameRequest request) {
        // Получаем ID текущего пользователя из контекста безопасности
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        // Преобразуем имя пользователя в ID (в данном случае предполагаем, что это число)
        Long userId = Long.parseLong(username);
        
        // Создаем игру через сервис
        GameDto gameDto = gameService.createSinglePlayerGame(userId, request);
        
        return ResponseEntity.ok(gameDto);
    }
}
