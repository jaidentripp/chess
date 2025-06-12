package websocket;

import chess.ChessBoard;
import com.google.gson.Gson;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import java.io.IOException;

@WebSocket
public class WebSocketServer {

    private static final Gson gson = new Gson();

    @OnWebSocketConnect
    public void onConnect(Session session) {
        // Optionally log connection
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        // Optionally log close
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) throws IOException {
        UserGameCommand command = gson.fromJson(message, UserGameCommand.class);

        if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {
            ChessBoard game = new ChessBoard(); // Make sure this is NOT null and initializes a valid board!
            game.resetBoard();
            String playerColor = "WHITE"; // or derive from command

            ServerMessage response = new ServerMessage(ServerMessage.ServerMessageType.LOAD_GAME, game, playerColor);
            session.getRemote().sendString(gson.toJson(response));
        }

//        try {
//            UserGameCommand command = gson.fromJson(message, UserGameCommand.class);
//
//            if (command.getCommandType() == UserGameCommand.CommandType.CONNECT) {
//                ChessBoard board = new ChessBoard(); // Ensure this works
//                String playerColor = "WHITE"; // Or derive from command
//
//                ServerMessage response = new ServerMessage(
//                        ServerMessage.ServerMessageType.LOAD_GAME,
//                        board,
//                        playerColor
//                );
//                session.getRemote().sendString(gson.toJson(response));
//            }
//        } catch (Exception e) {
//            // Send an ERROR message if something fails
//            ServerMessage error = new ServerMessage(
//                    ServerMessage.ServerMessageType.ERROR,
//                    "Failed to process command: " + e.getMessage()
//            );
//            session.getRemote().sendString(gson.toJson(error));
//        }
    }
}