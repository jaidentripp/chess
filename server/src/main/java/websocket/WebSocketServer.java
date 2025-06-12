package websocket;

//import javax.websocket.*;
//import javax.websocket.server.ServerEndpoint;
//import com.google.gson.Gson;
//import org.eclipse.jetty.server.session.Session;
//import websocket.commands.UserGameCommand;
//import websocket.messages.ServerMessage;
//import chess.ChessBoard;
//import chess.ChessGame;
//import java.util.*;
//import java.io.IOException;

//@ServerEndpoint("/ws")
public class WebSocketServer {
//    private static final Gson gson = new Gson();
//
//    // Track sessions per gameID
//    private static final Map<Integer, Set<Session>> gameSessions = new HashMap<>();
//    // Track game state per gameID (replace with your DB or game manager as needed)
//    private static final Map<Integer, ChessGame> games = new HashMap<>();
//
//    @OnOpen
//    public void onOpen(Session session) {
//        // Wait for CONNECT command
//    }
//
//    @OnMessage
//    public void onMessage(String message, Session session) throws IOException {
//        UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
//        if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {
//            int gameID = command.getGameID();
//            // Add session to game
//            gameSessions.computeIfAbsent(gameID, k -> new HashSet<>()).add(session);
//
//            // Get or create game state
//            ChessGame game = games.computeIfAbsent(gameID, k -> new ChessGame());
//            ChessBoard board = game.getBoard();
//
//            // For this test, just send LOAD_GAME to the connecting client
//            ServerMessage loadMsg = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, board, "white"); // or "black" or null as needed
//            session.getBasicRemote().sendText(gson.toJson(loadMsg));
//        }
//        // Add other command handlers (MAKE_MOVE, LEAVE, RESIGN) as needed for more tests
//    }
//
//    @OnClose
//    public void onClose(Session session, CloseReason reason) {
//        // Remove session from all games
//        for (Set<Session> sessions : gameSessions.values()) {
//            sessions.remove(session);
//        }
//    }
//
//    @OnError
//    public void onError(Session session, Throwable throwable) {
//        System.err.println("WebSocket error: " + throwable.getMessage());
//    }
}