package websocket;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

import javax.websocket.*;
import java.util.function.Consumer;

@ClientEndpoint
public class WebSocketClient {
    private Session session;
    private final Gson gson = new Gson();
    private Consumer<ServerMessage> messageHandler;

    public WebSocketClient(String serverUri, Consumer<ServerMessage> messageHandler) {
        this.messageHandler = messageHandler;
        try {
            WebSocketClient container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(serverUri));
        } catch (Exception e) {
            throw new RuntimeException("Websocket connection failed", e);
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("Connected to WebSocket server");
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
            messageHandler.accept(serverMessage);
        } catch (Exception e) {
            System.err.println("Error parsing server message: " + e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Connection closed: " + reason.toString());
        this.session = null;
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket error: " + throwable.getMessage());
    }

    public void sendCommand(UserGameCommand command) {
        if (session != null && session.isOpen()) {
            String json = gson.toJson(command);
            session.getAsyncRemote().sendText(json);
        } else {
            System.err.println("Cannot send command - WebSocket not connected");
        }
    }



    //for testing
    public static void main(String[] args) {
        WebSocketClient client = new WebSocketClient(
                "ws://localhost:8080/chess/game",
                message -> System.out.println("Received: " + message.getServerMessageType())
        );


    }
}
