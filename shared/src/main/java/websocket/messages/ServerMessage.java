package websocket.messages;

import chess.ChessBoard;

import java.util.List;
import java.util.Objects;

/**
 * Represents a Message the server can send through a WebSocket
 * 
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {
    private final ServerMessageType serverMessageType;

    private String message;
    private ChessBoard game;
    private String playerColor;
    private List<String> legalMoves;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
    }

    //for notifications and errors
    public ServerMessage(ServerMessageType type, String message) {
        this.serverMessageType = type;
        this.message = message;
    }

    //for board updates LOAD_GAME
    public ServerMessage(ServerMessageType type, ChessBoard game, String playerColor) {
        this.serverMessageType = type;
        this.game = game;
        this.playerColor = playerColor;
    }

    //for sending legal moves
    public ServerMessage(ServerMessageType type, List<String> legalMoves) {
        this.serverMessageType = type;
        this.legalMoves = legalMoves;
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    public String getMessage() {
        return message;
    }

    public ChessBoard getGame() {
        return game;
    }

    public String getPlayerColor() {
        return playerColor;
    }

    public List<String> getLegalMoves() {
        return legalMoves;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServerMessage)) {
            return false;
        }
        ServerMessage that = (ServerMessage) o;
        return getServerMessageType() == that.getServerMessageType() &&
                Objects.equals(getMessage(), that.getMessage()) &&
                Objects.equals(getGame(), that.getGame()) &&
                Objects.equals(getPlayerColor(), that.getPlayerColor()) &&
                Objects.equals(getLegalMoves(), that.getLegalMoves());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType(), getMessage(), getGame(), getPlayerColor(), getLegalMoves());
    }
}
