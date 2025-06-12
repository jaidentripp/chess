package websocket;

import chess.ChessBoard;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
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
    private static final ConcurrentHashMap<Integer, Set<Session>> gameSessions = new ConcurrentHashMap<>();
    // Map session to gameID for cleanup
    private static final ConcurrentHashMap<Session, Integer> sessionGameMap = new ConcurrentHashMap<>();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        // Optionally log connection
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        Integer gameID = sessionGameMap.remove(session);
        if (gameID != null) {
            Set<Session> sessions = gameSessions.get(gameID);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    gameSessions.remove(gameID);
                }
            }
        }
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        UserGameCommand command = GSON.fromJson(message, UserGameCommand.class);

        if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {

            String authToken = command.getAuthToken(); // <-- get from command

            // 1. Validate auth token
            try {
                if (gameService == null || gameService.dao.getAuth(authToken) == null) {
                    ServerMessage errorMsg = new ServerMessage(
                            ServerMessage.ServerMessageType.ERROR,
                            "Invalid or missing authentication token."
                    );
                    session.getRemote().sendString(GSON.toJson(errorMsg));
                    return;
                }
            } catch (DataAccessException e) {
                ServerMessage errorMsg = new ServerMessage(
                        ServerMessage.ServerMessageType.ERROR,
                        "Data access error while checking authentication."
                );
                session.getRemote().sendString(GSON.toJson(errorMsg));
                return;
            }

            int gameID = command.getGameID(); // Ensure this is present in the command

            if (!isValidGameID(gameID)) {
                // Send ERROR message to this session
                ServerMessage errorMsg = new ServerMessage(
                        ServerMessage.ServerMessageType.ERROR,
                        "Invalid game ID: " + gameID
                );
                session.getRemote().sendString(GSON.toJson(errorMsg));
                return;
            }

            gameSessions.putIfAbsent(gameID, new CopyOnWriteArraySet<>());
            Set<Session> sessions = gameSessions.get(gameID);
            sessions.add(session);
            sessionGameMap.put(session, gameID);

            ChessBoard game = new ChessBoard();
            game.resetBoard();
            String playerColor = "WHITE"; // Or derive from command

            // 1. Send LOAD_GAME only to the connecting session
            ServerMessage loadGameMsg = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, game, playerColor);
            session.getRemote().sendString(GSON.toJson(loadGameMsg));

            // 2. Send NOTIFICATION to all other sessions in the game
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
    }

//    private boolean isValidGameID(int gameID) {
//        try {
//            return gameService.dao.getGame(gameID) != null;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    private boolean isValidGameID(int gameID) {
        return gameService != null && gameService.gameExists(gameID);
    }
}