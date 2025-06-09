package websocket.messages;

import chess.ChessGame;

import java.util.Objects;

/**
 * Represents a Message the server can send through a WebSocket
 * 
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class ServerMessage {
    private final ServerMessageType serverMessageType;

    //only used for LOAD_GAME
    private final ChessGame game;

    //only used for ERROR
    private final String errorMessage;

    //only used for NOTIFICATION
    private final String message;

    public enum ServerMessageType {
        LOAD_GAME,
        ERROR,
        NOTIFICATION
    }

    //constructor for LOAD_GAME
    public ServerMessage(ChessGame game) {
        this.serverMessageType = ServerMessageType.LOAD_GAME;
        this.game = game;
        this.errorMessage = null;
        this.message = null;
    }

    //constructor for ERROR
    public ServerMessage(String errorMessage) {
        this.serverMessageType = ServerMessageType.ERROR;
        this.errorMessage = errorMessage;
        this.game = null;
        this.message = null;
    }

    //constructor for NOTIFICATION
    public ServerMessage(ServerMessageType type, String message) {
        if (type != ServerMessageType.NOTIFICATION) {
            throw new IllegalArgumentException("Use this constructor only for NOTIFICATION type");
        }
        this.serverMessageType = type;
        this.message = message;
        this.game = null;
        this.errorMessage = null;
    }

    public ServerMessage(ServerMessageType type) {
        this.serverMessageType = type;
        this.game = null;
        this.errorMessage = null;
        this.message = null;
    }

    public ServerMessageType getServerMessageType() {
        return this.serverMessageType;
    }

    public ChessGame getGame() {
        return game;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getMessage() {
        return message;
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
        return getServerMessageType() == that.getServerMessageType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getServerMessageType());
    }
}
