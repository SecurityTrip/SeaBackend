package ru.securitytrip.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.securitytrip.backend.model.GameBoard;

import java.util.List;

@Repository
public interface GameBoardRepository extends JpaRepository<GameBoard, Long> {
    List<GameBoard> findByGameId(Long gameId);
    List<GameBoard> findByOwnerId(Long ownerId);
} 