package ru.securitytrip.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.securitytrip.backend.model.MultiplayerRoomEntity;

public interface MultiplayerRoomRepository extends JpaRepository<MultiplayerRoomEntity, String> {
    // поиск по коду комнаты уже реализован через findById
} 