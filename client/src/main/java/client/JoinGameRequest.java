package client;

public class JoinGameRequest {
    public int gameID;
    public String playerColor;

    public JoinGameRequest(int gameID, String playerColor) {
        this.gameID = gameID;
        this.playerColor = playerColor;
    }
}
