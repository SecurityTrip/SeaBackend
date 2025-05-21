package ru.securitytrip.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "multiplayer_rooms")
public class MultiplayerRoomEntity {
    @Id
    private String code;

    private Long player1Id;
    private Long player2Id;

    @Lob
    private String player1ShipsJson;
    @Lob
    private String player2ShipsJson;

    @Lob
    private String player1BoardJson;
    @Lob
    private String player2BoardJson;

    private String currentTurn; // "player1" или "player2"

    @Lob
    private String gameStateJson; // сериализованный GameDto

    @Enumerated(EnumType.STRING)
    private GameMode gameMode;
} 