package ru.securitytrip.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "game_board")
public class GameBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;
    
    private boolean isComputer;
    
    // Двумерный массив состояний клеток (0 - пусто, 1 - корабль, 2 - промах, 3 - попадание)
    @Column(length = 10000)
    private String boardState;
    
    // Информация о расположении кораблей (формат JSON)
    @Column(length = 10000)
    private String shipsData;
    
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "game_id") // Внешний ключ в таблице game_board
    private Game game;
    
    // Метод инициализации пустой доски 10x10
    public void initEmptyBoard() {
        // Создаем пустое поле 10x10 (все клетки = 0)
        int[][] emptyBoard = new int[10][10];
        this.boardState = convertBoardToString(emptyBoard);
    }
    
    // Вспомогательный метод для конвертации двумерного массива в строку
    public String convertBoardToString(int[][] board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                sb.append(board[i][j]);
                if (j < board[i].length - 1) {
                    sb.append(",");
                }
            }
            if (i < board.length - 1) {
                sb.append(";");
            }
        }
        return sb.toString();
    }
    
    // Вспомогательный метод для конвертации строки в двумерный массив
    public int[][] getBoardAsArray() {
        if (boardState == null || boardState.isEmpty()) {
            return new int[10][10];
        }
        
        String[] rows = boardState.split(";");
        int[][] board = new int[rows.length][];
        
        for (int i = 0; i < rows.length; i++) {
            String[] cells = rows[i].split(",");
            board[i] = new int[cells.length];
            for (int j = 0; j < cells.length; j++) {
                board[i][j] = Integer.parseInt(cells[j]);
            }
        }
        
        return board;
    }

    public void setBoardState(String boardState) {
        this.boardState = boardState;
    }
    
    public String getBoardState() {
        return this.boardState;
    }
    
    public void setShipsData(String shipsData) {
        this.shipsData = shipsData;
    }
    
    public String getShipsData() {
        return this.shipsData;
    }
}
