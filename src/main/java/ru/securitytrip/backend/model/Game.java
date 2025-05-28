package ru.securitytrip.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Setter
@Getter
@Entity
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private GameMode mode;
    
    @Enumerated(EnumType.STRING)
    private GameState gameState = GameState.WAITING;
    
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel = DifficultyLevel.MEDIUM;
    
    // Координаты последнего удачного выстрела (для ИИ сложного уровня)
    private int lastHitX = -1;
    private int lastHitY = -1;

    // Список всех непотопленных попаданий (для умного добивания)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "game_pending_hits", joinColumns = @JoinColumn(name = "game_id"))
    private List<PendingHit> pendingHits = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    public static class PendingHit {
        private int x;
        private int y;

        public PendingHit() {}
        public PendingHit(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PendingHit that = (PendingHit) o;
            return x == that.x && y == that.y;
        }

        @Override
        public int hashCode() {
            return 31 * x + y;
        }
    }
    
    // true - ход игрока, false - ход компьютера
    private boolean playerTurn = true;

    // Связь "одна игра содержит две доски"
    @OneToMany(
            mappedBy = "game", // Ссылаемся на поле "game" в классе GameBoard
            cascade = CascadeType.ALL,
            orphanRemoval = true // Удаляет доски при удалении игры
    )
    private List<GameBoard> boards = new ArrayList<>();

    // Метод для добавления досок с автоматической связью
    public void addBoard(GameBoard board) {
        if (boards.size() < 2) { // Ограничение на максимум 2 доски
            boards.add(board);
            board.setGame(this);
        } else {
            throw new IllegalStateException("Game can only have 2 boards");
        }
    }
    
    // Получить доску игрока
    public GameBoard getPlayerBoard() {
        for (GameBoard board : boards) {
            if (!board.isComputer()) {
                return board;
            }
        }
        return null;
    }
    
    // Получить доску компьютера
    public GameBoard getComputerBoard() {
        for (GameBoard board : boards) {
            if (board.isComputer()) {
                return board;
            }
        }
        return null;
    }
    
    // Переключение хода
    public void toggleTurn() {
        this.playerTurn = !this.playerTurn;
    }
}
