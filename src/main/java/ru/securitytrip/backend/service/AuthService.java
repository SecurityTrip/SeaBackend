package ru.securitytrip.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.securitytrip.backend.dto.LoginRequest;
import ru.securitytrip.backend.dto.LoginResponse;
import ru.securitytrip.backend.dto.RegisterRequest;
import ru.securitytrip.backend.dto.RegisterResponse;
import ru.securitytrip.backend.jwt.JwtUtils;
import ru.securitytrip.backend.model.User;
import ru.securitytrip.backend.repository.UserRepository;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    public LoginResponse authenticateUser(LoginRequest loginRequest) {
        logger.debug("Попытка аутентификации пользователя: {}", loginRequest.getUsername());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        logger.debug("JWT токен успешно сгенерирован");

        User user = userRepository.findByUsername(loginRequest.getUsername()).orElseThrow();
        logger.debug("Пользователь найден в базе данных, userId: {}", user.getId());
        
        return new LoginResponse(jwt, user.getUsername(), user.getId());
    }

    public RegisterResponse registerUser(RegisterRequest registerRequest) {
        logger.debug("Попытка регистрации пользователя: {}", registerRequest.getUsername());
        
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            logger.warn("Пользователь с именем {} уже существует", registerRequest.getUsername());
            return new RegisterResponse("Пользователь с таким именем уже существует", false);
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setAvatarId(registerRequest.getAvatarId());
        
        logger.debug("Сохранение нового пользователя в базу данных");
        userRepository.save(user);
        logger.info("Пользователь {} успешно зарегистрирован", registerRequest.getUsername());

        return new RegisterResponse("Пользователь успешно зарегистрирован", true);
    }
} 