package ru.securitytrip.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.securitytrip.backend.service.UserService;

@RestController
@RequestMapping("/user")
@Tag(name = "Морской Бой", description = "API для управления профилем пользователя")
public class UserController {
    @Autowired
    private UserService userService;
}
