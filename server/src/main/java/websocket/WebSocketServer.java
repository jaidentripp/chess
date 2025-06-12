package websocket;

import chess.*;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
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
        System.out.println("Raw message: " + message);

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

        // 3. Command handling
        switch (command.getCommandType()) {
            case CONNECT: {
                // Track session
                gameSessions.putIfAbsent(gameID, new CopyOnWriteArraySet<>());
                Set<Session> sessions = gameSessions.get(gameID);
                sessions.add(session);
                sessionGameMap.put(session, gameID);

                // Get game and player color (derive from command or auth if needed)
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
                            playerColor = "OBSERVER"; // or null, or handle as needed
                        }
                    }
                } catch (Exception e) {
                    playerColor = null; // or handle error
                }

                // Send LOAD_GAME to connecting session
                ServerMessage loadGameMsg = new ServerMessage(
                        ServerMessage.ServerMessageType.LOAD_GAME,
                        gameBoard,
                        playerColor
                );
                session.getRemote().sendString(GSON.toJson(loadGameMsg));

                // Send NOTIFICATION to others
                ServerMessage notificationMsg = new ServerMessage(
                        ServerMessage.ServerMessageType.NOTIFICATION,
                        playerColor + " joined the game!"
                );
                for (Session s : sessions) {
                    if (s != session && s.isOpen()) {
                        s.getRemote().sendString(GSON.toJson(notificationMsg));
                    }
                }
                break;
            }
            case MAKE_MOVE: {
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

                ChessGame chessGame = gameData.game();
                ChessBoard board = chessGame.getBoard();

                ChessMove move = command.getMove();

                if (move == null) {
                    ServerMessage errorMsg = new ServerMessage(
                            ServerMessage.ServerMessageType.ERROR,
                            "Move not provided."
                    );
                    session.getRemote().sendString(GSON.toJson(errorMsg));
                    return;
                }

                // 1. Get the username from the auth token
                String username = null;
                try {
                    AuthData auth = gameService.dao.getAuth(authToken);
                    if (auth != null) {
                        username = auth.username();
                    }
                } catch (Exception e) {
                    // handle as needed
                }

// 2. Determine the player's color in this game
                ChessGame.TeamColor playerColor = null;
                if (username != null) {
                    if (username.equals(gameData.whiteUsername())) {
                        playerColor = ChessGame.TeamColor.WHITE;
                    } else if (username.equals(gameData.blackUsername())) {
                        playerColor = ChessGame.TeamColor.BLACK;
                    }
                }

                System.out.println("Player: " + username + " color: " + playerColor + " teamTurn: " + chessGame.getTeamTurn());
                System.out.println("Move from: " + move.getStartPosition() + " to: " + move.getEndPosition());
                ChessPiece printPiece = chessGame.getBoard().getPiece(move.getStartPosition());
                System.out.println("Piece at from: " + printPiece);

// 3. Check if it's their turn and if the piece matches their color
                boolean validMove = false;
                if (playerColor != null && chessGame.getTeamTurn() == playerColor) {
                    ChessPosition from = move.getStartPosition();
                    ChessPiece piece = chessGame.getBoard().getPiece(from);
                    if (piece != null && piece.getTeamColor() == playerColor) {
                        validMove = true;
                    }
                }

                if (!validMove) {
                    ServerMessage errorMsg = new ServerMessage(
                            ServerMessage.ServerMessageType.ERROR,
                            "You cannot move for your opponent."
                    );
                    session.getRemote().sendString(GSON.toJson(errorMsg));
                    return;
                }

                // Attempt to apply move
                try {
                    chessGame.makeMove(move);
                } catch (Exception e) {
                    ServerMessage errorMsg = new ServerMessage(
                            ServerMessage.ServerMessageType.ERROR,
                            "Invalid move."
                    );
                    session.getRemote().sendString(GSON.toJson(errorMsg));
                    return;
                }

                // Save updated game state
                try {
                    gameService.dao.updateGame(new GameData(
                            gameData.gameID(),
                            gameData.whiteUsername(),
                            gameData.blackUsername(),
                            gameData.gameName(),
                            chessGame
                    ));
                } catch (Exception e) {
                    ServerMessage errorMsg = new ServerMessage(
                            ServerMessage.ServerMessageType.ERROR,
                            "Failed to update game after move."
                    );
                    session.getRemote().sendString(GSON.toJson(errorMsg));
                    return;
                }

                // Broadcast updated board to all sessions in this game
                Set<Session> sessions = gameSessions.get(gameID);
                ServerMessage loadGameMsg = new ServerMessage(
                        ServerMessage.ServerMessageType.LOAD_GAME,
                        board,
                        null // or derive player color if you want
                );
                for (Session s : sessions) {
                    if (s.isOpen()) {
                        s.getRemote().sendString(GSON.toJson(loadGameMsg));
                    }
                }

                // Send NOTIFICATION to all except the mover (session)
                String moverUsername = null;
                try {
                    AuthData auth = gameService.dao.getAuth(authToken);
                    if (auth != null) {
                        moverUsername = auth.username();
                    }
                } catch (Exception e) {
                    // ignore, fallback to null
                }

                String moveDesc = String.format(
                        "%s moved from %s to %s",
                        (moverUsername != null ? moverUsername : "A player"),
                        // Convert move start/end to algebraic notation for human readability
                        toAlgebraicWebSocket(move.getStartPosition()),
                        toAlgebraicWebSocket(move.getEndPosition())
                );

                ServerMessage notifyMsg = new ServerMessage(
                        ServerMessage.ServerMessageType.NOTIFICATION,
                        moveDesc
                );

                for (Session s : sessions) {
                    if (s != session && s.isOpen()) {
                        s.getRemote().sendString(GSON.toJson(notifyMsg));
                    }
                }

                break;
            }
            case RESIGN: {
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
                Set<Session> sessions = gameSessions.get(gameID);
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
                break;
            }
            case LEAVE: {
                //remove the session from the game
                Set<Session> sessions = gameSessions.get(gameID);
                if (sessions != null) {
                    sessions.remove(session);
                }
                sessionGameMap.remove(session);

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

                // Save the updated GameData
                try {
                    gameService.dao.updateGame(new GameData(
                            gameData.gameID(),
                            newWhite,
                            newBlack,
                            gameData.gameName(),
                            gameData.game()
                    ));
                } catch (Exception e) {
                    // ignore or handle error
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
                break;
            }
        }
    }

    private boolean isValidGameID(int gameID) {
        return gameService != null && gameService.gameExists(gameID);
    }

    private String toAlgebraicWebSocket(ChessPosition pos) {
        char file = (char) ('a' + pos.getColumn() - 1);
        char rank = (char) ('0' + pos.getRow());
        return "" + file + rank;
    }
}