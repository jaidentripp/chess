package websocket.commands;

import java.util.Objects;

/**
 * Represents a command a user can send the server over a websocket
 *
 * Note: You can add to this class, but you should not alter the existing
 * methods.
 */
public class UserGameCommand {

    private final CommandType commandType;

    private final String authToken;

    private final Integer gameID;

    //for MAKE_MOVE
    private String moveFrom;
    private String moveTo;

    //for highlight legal moves
    private String selectedSquare;

    //constructor for CONNECT, LEAVE, RESIGN
    public UserGameCommand(CommandType commandType, String authToken, Integer gameID) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
    }

    //constructor for MAKE_MOVE
    public UserGameCommand(CommandType commandType, String authToken, Integer gameID, String moveFrom, String moveTo) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        this.moveFrom = moveFrom;
        this.moveTo = moveTo;
    }

    //for highlight legal moves
    public UserGameCommand(CommandType commandType, String authToken, Integer gameID, String selectedSquare) {
        this.commandType = commandType;
        this.authToken = authToken;
        this.gameID = gameID;
        this.selectedSquare = selectedSquare;
    }

    public enum CommandType {
        CONNECT,
        MAKE_MOVE,
        LEAVE,
        RESIGN
    }

    public CommandType getCommandType() {
        return commandType;
    }

    public String getAuthToken() {
        return authToken;
    }

    public Integer getGameID() {
        return gameID;
    }

    public String getMoveFrom() {
        return moveFrom;
    }

    public String getMoveTo() {
        return moveTo;
    }

    public String getSelectedSquare() {
        return selectedSquare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserGameCommand)) {
            return false;
        }
        UserGameCommand that = (UserGameCommand) o;
        return getCommandType() == that.getCommandType() &&
                Objects.equals(getAuthToken(), that.getAuthToken()) &&
                Objects.equals(getGameID(), that.getGameID()) &&
                Objects.equals(getMoveFrom(), that.getMoveFrom()) &&
                Objects.equals(getMoveTo(), that.getMoveTo()) &&
                Objects.equals(getSelectedSquare(), that.getSelectedSquare());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCommandType(), getAuthToken(), getGameID(),
                getMoveFrom(), getMoveTo(), getSelectedSquare());
    }
}
