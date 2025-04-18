package ru.securitytrip.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.securitytrip.backend.model.Game;
import ru.securitytrip.backend.model.GameMode;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    @Query("SELECT g FROM Game g JOIN g.boards b WHERE b.ownerId = :userId")
    List<Game> findGamesForUser(Long userId);
    
    List<Game> findByMode(GameMode mode);
} 