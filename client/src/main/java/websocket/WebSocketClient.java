package websocket;

import chess.ChessBoard;
import chess.ChessGame;
import ui.ChessBoardPrinter;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;
import com.google.gson.Gson;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

import java.net.URI;
import java.net.URISyntaxException;

@ClientEndpoint
public class WebSocketClient {
    private Session session;
    private final Gson gson = new Gson();

    public WebSocketClient(String serverDomain) throws Exception {
        URI uri = new URI("ws://" + serverDomain + "/ws");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, uri);
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to WebSocket server");
        this.session = session;
        // Optionally, send an initial CONNECT command here
        // UserGameCommand connectCmd = new UserGameCommand(UserGameCommand.CommandType.CONNECT, "authToken", 123);
        // sendCommand(connectCmd);
    }

//    @OnMessage
//    public void onMessage(String message) {
//        ServerMessage serverMsg = gson.fromJson(message, ServerMessage.class);
//        switch (serverMsg.getServerMessageType()) {
//            case LOAD_GAME -> {
//                System.out.println("LOAD_GAME: " + serverMsg.getBoard());
//                // Update your UI or game state here
//            }
//            case NOTIFICATION -> {
//                System.out.println("NOTIFICATION: " + serverMsg.getMessage());
//            }
//            case ERROR -> {
//                System.err.println("ERROR: " + serverMsg.getMessage());
//            }
//        }
//    }

    @Override
    public void onMessage(String message) {
        ServerMessage serverMsg = gson.fromJson(message, ServerMessage.class);
        switch (serverMsg.getServerMessageType()) {
            case LOAD_GAME -> {
                ChessBoard board = serverMsg.getBoard();
                String playerColor = serverMsg.getPlayerColor();
                //ChessGame game = serverMsg.getGame();
                // Update your UI here (e.g., redraw board)
                ChessBoardPrinter.printBoard(board, "white".equalsIgnoreCase(playerColor));
            }
            case NOTIFICATION -> System.out.println("[NOTIFICATION] " + serverMsg.getMessage());
            case ERROR -> System.err.println("[ERROR] " + serverMsg.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("WebSocket closed: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
    }

    // Send a UserGameCommand to the server
    public void sendCommand(UserGameCommand command) {
        String json = gson.toJson(command);
        session.getAsyncRemote().sendText(json);
    }

    // Optionally, send a raw message
    public void sendMessage(String message) {
        session.getAsyncRemote().sendText(message);
    }
}
