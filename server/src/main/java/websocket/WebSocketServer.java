package websocket;

import chess.*;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import service.GameService;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@WebSocket
public class WebSocketServer {

    private static final Gson GSON = new Gson();

    private static GameService gameService;

    public static void setGameService(GameService service) {
        gameService = service;
    }
    // Map gameID to sessions
    private static final ConcurrentHashMap<Integer, Set<Session>> GAME_SESSIONS = new ConcurrentHashMap<>();
    // Map session to gameID for cleanup
    private static final ConcurrentHashMap<Session, Integer> SESSION_GAME_MAP = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        // Optionally log connection
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        Integer gameID = SESSION_GAME_MAP.remove(session);
        if (gameID != null) {
            Set<Session> sessions = GAME_SESSIONS.get(gameID);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    GAME_SESSIONS.remove(gameID);
                }
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        UserGameCommand command = GSON.fromJson(message, UserGameCommand.class);

        // Extract common fields
        int gameID = command.getGameID();
        String authToken = command.getAuthToken();

        // 1. Auth check
        try {
            if (gameService == null || gameService.dao.getAuth(authToken) == null) {
                ServerMessage errorMsg = new ServerMessage(
                        ServerMessage.ServerMessageType.ERROR,
                        "Invalid or missing authentication token."
                );
                session.getRemote().sendString(GSON.toJson(errorMsg));
                return;
            }
        } catch (Exception e) {
            ServerMessage errorMsg = new ServerMessage(
                    ServerMessage.ServerMessageType.ERROR,
                    "Data access error while checking authentication."
            );
            session.getRemote().sendString(GSON.toJson(errorMsg));
            return;
        }

        // 2. GameID check
        if (!isValidGameID(gameID)) {
            ServerMessage errorMsg = new ServerMessage(
                    ServerMessage.ServerMessageType.ERROR,
                    "Invalid game ID: " + gameID
            );
            session.getRemote().sendString(GSON.toJson(errorMsg));
            return;
        }

        // Command handling
        switch (command.getCommandType()) {
            case CONNECT:
                handleConnect(session, command, gameID, authToken);
                break;
            case MAKE_MOVE:
                handleMakeMove(session, command, gameID, authToken);
                break;
            case RESIGN:
                handleResign(session, command, gameID, authToken);
                break;
            case LEAVE:
                handleLeave(session, command, gameID, authToken);
                break;
            default:
                sendError(session, "Unknown command type.");
                break;
        }
    }

    private void handleConnect(Session session, UserGameCommand command, int gameID, String authToken)
            throws IOException {
        //track session
        GAME_SESSIONS.putIfAbsent(gameID, new CopyOnWriteArraySet<>());
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        sessions.add(session);
        SESSION_GAME_MAP.put(session, gameID);

        //get game and player color (derive from command or auth if needed)
        GameData gameData;
        try {
            gameData = gameService.dao.getGame(gameID);
        } catch (Exception e) {
            ServerMessage errorMsg = new ServerMessage(
                    ServerMessage.ServerMessageType.ERROR,
                    "Data access error while fetching game."
            );
            session.getRemote().sendString(GSON.toJson(errorMsg));
            return;
        }

        ChessBoard gameBoard = gameData.game().getBoard();

        String playerColor = null;
        try {
            AuthData auth = gameService.dao.getAuth(authToken);
            GameData gameInfo = gameService.dao.getGame(gameID);
            if (auth != null && gameInfo != null) {
                if (auth.username().equals(gameInfo.whiteUsername())) {
                    playerColor = "WHITE";
                } else if (auth.username().equals(gameInfo.blackUsername())) {
                    playerColor = "BLACK";
                } else {
                    playerColor = "OBSERVER";
                }
            }
        } catch (Exception e) {
            playerColor = null;
        }
        //send LOAD_GAME to connecting session
        ServerMessage loadGameMsg = new ServerMessage(
                ServerMessage.ServerMessageType.LOAD_GAME,
                gameBoard,
                playerColor
        );
        session.getRemote().sendString(GSON.toJson(loadGameMsg));
        //send NOTIFICATION to others
        ServerMessage notificationMsg = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                playerColor + " joined the game!"
        );
        for (Session s : sessions) {
            if (s != session && s.isOpen()) {
                s.getRemote().sendString(GSON.toJson(notificationMsg));
            }
        }
    }

    private void handleMakeMove(Session session, UserGameCommand command, int gameID, String authToken) throws IOException {
        GameData gameData = fetchGameData(session, gameID);
        if (gameData == null) return;

        ChessGame chessGame = gameData.game();
        ChessBoard board = chessGame.getBoard();
        ChessMove move = command.getMove();

        if (move == null) {
            sendError(session, "Move not provided.");
            return;
        }
        String username = fetchUsername(authToken);
        ChessGame.TeamColor playerColor = getPlayerColor(username, gameData);
        if (!isValidMoveAttempt(chessGame, move, playerColor)) {
            sendError(session, "You cannot move for your opponent.");
            return;
        }
        if (!applyMove(session, chessGame, move)){
            return;
        }
        if (!updateGameState(session, gameService, gameData, chessGame)){
            return;
        }
        broadcastBoard(gameID, board);
        notifyMove(gameID, session, move, authToken);
    }

    private void handleResign(Session session, UserGameCommand command, int gameID, String authToken)
            throws IOException {
        //get game and player info
        GameData gameData;
        try {
            gameData = gameService.dao.getGame(gameID);
        } catch (Exception e) {
            ServerMessage errorMsg = new ServerMessage(
                    ServerMessage.ServerMessageType.ERROR,
                    "Data access error while fetching game."
            );
            session.getRemote().sendString(GSON.toJson(errorMsg));
            return;
        }
        //game is already over can't resign
        if (gameData.game().isGameOver()) {
            ServerMessage errorMsg = new ServerMessage(
                    ServerMessage.ServerMessageType.ERROR,
                    "Game is already over."
            );
            session.getRemote().sendString(GSON.toJson(errorMsg));
            return;
        }
        String username = null;
        try {
            AuthData auth = gameService.dao.getAuth(authToken);
            if (auth != null) {
                username = auth.username();
            }
        } catch (Exception e) {
            //handle
        }
        //determine player's color
        ChessGame.TeamColor playerColor = null;
        if (username != null) {
            if (username.equals(gameData.whiteUsername())) {
                playerColor = ChessGame.TeamColor.WHITE;
            } else if (username.equals(gameData.blackUsername())) {
                playerColor = ChessGame.TeamColor.BLACK;
            }
        }
        //only allow resign if the user is a player (not an observer)
        if (playerColor == null) {
            ServerMessage errorMsg = new ServerMessage(
                    ServerMessage.ServerMessageType.ERROR,
                    "Observers cannot resign."
            );
            session.getRemote().sendString(GSON.toJson(errorMsg));
            return;
        }
        gameData.game().setGameOver(true);
        //save updated game state
        try {
            gameService.dao.updateGame(new GameData(
                    gameData.gameID(),
                    gameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    gameData.game()
            ));
        } catch (Exception e) {
            ServerMessage errorMsg = new ServerMessage(
                    ServerMessage.ServerMessageType.ERROR,
                    "Failed to update game after resign."
            );
            session.getRemote().sendString(GSON.toJson(errorMsg));
            return;
        }
        //notify all players
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        String resignMsg = (playerColor != null ? playerColor : username) + " resigned. Game over!";
        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                resignMsg
        );
        for (Session s : sessions) {
            if (s.isOpen()) {
                s.getRemote().sendString(GSON.toJson(notification));
            }
        }
    }

    private void handleLeave(Session session, UserGameCommand command, int gameID, String authToken)
            throws IOException {
        //remove the session from the game
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        if (sessions != null) {
            sessions.remove(session);
        }
        SESSION_GAME_MAP.remove(session);
        //get username for the notification
        String username = null;
        try {
            AuthData auth = gameService.dao.getAuth(authToken);
            if (auth != null) {
                username = auth.username();
            }
        } catch (Exception e) {
            //ignore
        }
        GameData gameData = null;
        try {
            gameData = gameService.dao.getGame(gameID);
        } catch (Exception e) {
            // ignore
        }
        String newWhite = gameData.whiteUsername();
        String newBlack = gameData.blackUsername();
        if (username != null && gameData != null) {
            if (username.equals(gameData.whiteUsername())) {
                newWhite = null;
            } else if (username.equals(gameData.blackUsername())) {
                newBlack = null;
            }
        }
        //save the updated GameData
        try {
            gameService.dao.updateGame(new GameData(
                    gameData.gameID(),
                    newWhite,
                    newBlack,
                    gameData.gameName(),
                    gameData.game()
            ));
        } catch (Exception e) {
            //ignore
        }
        //notification message
        String leaveMsg = (username != null ? username : "A player") + " left the game!";
        ServerMessage notification = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                leaveMsg
        );
        //notify all other sessions (not the leaver)
        if (sessions != null) {
            for (Session s : sessions) {
                if (s != session && s.isOpen()) {
                    s.getRemote().sendString(GSON.toJson(notification));
                }
            }
        }
    }

    private void sendError(Session session, String message) throws IOException {
        ServerMessage errorMsg = new ServerMessage(ServerMessage.ServerMessageType.ERROR, message);
        session.getRemote().sendString(GSON.toJson(errorMsg));
    }

    private boolean isValidGameID(int gameID) {
        return gameService != null && gameService.gameExists(gameID);
    }

    private String toAlgebraicWebSocket(ChessPosition pos) {
        char file = (char) ('a' + pos.getColumn() - 1);
        char rank = (char) ('0' + pos.getRow());
        return "" + file + rank;
    }

    private GameData fetchGameData(Session session, int gameID) throws IOException {
        try {
            return gameService.dao.getGame(gameID);
        } catch (Exception e) {
            sendError(session, "Data access error while fetching game.");
            return null;
        }
    }

    private String fetchUsername(String authToken) {
        try {
            AuthData auth = gameService.dao.getAuth(authToken);
            return (auth != null) ? auth.username() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private ChessGame.TeamColor getPlayerColor(String username, GameData gameData) {
        if (username == null) return null;
        if (username.equals(gameData.whiteUsername())) return ChessGame.TeamColor.WHITE;
        if (username.equals(gameData.blackUsername())) return ChessGame.TeamColor.BLACK;
        return null;
    }

    private boolean isValidMoveAttempt(ChessGame chessGame, ChessMove move, ChessGame.TeamColor playerColor) {
        if (playerColor == null || chessGame.getTeamTurn() != playerColor) return false;
        ChessPiece piece = chessGame.getBoard().getPiece(move.getStartPosition());
        return piece != null && piece.getTeamColor() == playerColor;
    }

    private boolean applyMove(Session session, ChessGame chessGame, ChessMove move) throws IOException {
        try {
            chessGame.makeMove(move);
            return true;
        } catch (Exception e) {
            sendError(session, "Invalid move.");
            return false;
        }
    }

    private boolean updateGameState(Session session, GameService gameService, GameData gameData, ChessGame chessGame) throws IOException {
        try {
            gameService.dao.updateGame(new GameData(
                    gameData.gameID(),
                    gameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    chessGame
            ));
            return true;
        } catch (Exception e) {
            sendError(session, "Failed to update game after move.");
            return false;
        }
    }

    private void broadcastBoard(int gameID, ChessBoard board) throws IOException {
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        ServerMessage loadGameMsg = new ServerMessage(
                ServerMessage.ServerMessageType.LOAD_GAME,
                board,
                null
        );
        for (Session s : sessions) {
            if (s.isOpen()) {
                s.getRemote().sendString(GSON.toJson(loadGameMsg));
            }
        }
    }

    private void notifyMove(int gameID, Session moverSession, ChessMove move, String authToken) throws IOException {
        Set<Session> sessions = GAME_SESSIONS.get(gameID);
        String moverUsername = fetchUsername(authToken);

        String moveDesc = String.format(
                "%s moved from %s to %s",
                (moverUsername != null ? moverUsername : "A player"),
                toAlgebraicWebSocket(move.getStartPosition()),
                toAlgebraicWebSocket(move.getEndPosition())
        );

        ServerMessage notifyMsg = new ServerMessage(
                ServerMessage.ServerMessageType.NOTIFICATION,
                moveDesc
        );

        for (Session s : sessions) {
            if (s != moverSession && s.isOpen()) {
                s.getRemote().sendString(GSON.toJson(notifyMsg));
            }
        }
    }
}