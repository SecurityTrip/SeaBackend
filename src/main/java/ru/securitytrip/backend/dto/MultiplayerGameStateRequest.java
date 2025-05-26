package ru.securitytrip.backend.dto;

public class MultiplayerGameStateRequest {
    private String gameCode;
    private String userId;

    public MultiplayerGameStateRequest() {}

    public MultiplayerGameStateRequest(String gameCode, String userId) {
        this.gameCode = gameCode;
        this.userId = userId;
    }

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
