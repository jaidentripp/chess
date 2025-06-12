package client;

import chess.ChessBoard;
import chess.ChessMove;
import chess.ChessPiece;
import chess.ChessPosition;
import model.GameData;
import client.CreateGameResult;
import client.LoginResult;
import client.RegisterResult;
import ui.ChessBoardPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import websocket.WebSocketClient;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

public class ChessClient {
    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade server;
    private String authToken = null;
    private String username = null;
    private List<GameInfo> lastListedGames = new ArrayList<>();
    private WebSocketClient webSocketClient;
    private ChessBoard currentBoard;

    public ChessClient(String serverUrl) {
        this.server = new ServerFacade(serverUrl);
    }

    public void run() {
        //System.out.println("Welcome to 240 Chess Client");
        while (true) {
            if (authToken == null) {
                preLoginMenu();
            } else {
                postLoginMenu();
            }
        }
    }

    private void preLoginMenu() {
        System.out.println("\n[PreLogin] Enter command (help/register/login/quit): ");
        String command = scanner.nextLine().trim().toLowerCase();
        switch (command) {
            case "help" -> printPreLoginHelp();
            case "register" -> handleRegister();
            case "login" -> handleLogin();
            case "quit" -> {
                System.out.println("Goodbye!");
                System.exit(0);
            }
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    private void postLoginMenu() {
        System.out.print("[PostLogin] Enter command (help/logout/create game/list games/play game/observe game): ");
        String command = scanner.nextLine().trim().toLowerCase();
        switch (command) {
            case "help" -> printPostLoginHelp();
            case "logout" -> handleLogout();
            case "create game" -> handleCreateGame();
            case "list games" -> handleListGames();
            case "play game" -> handlePlayGame();
            case "observe game" -> handleObserveGame();
            default -> System.out.println("Unknown command. Type 'help' for options.");
        }
    }

    //Command handlers
    private void printPreLoginHelp() {
        System.out.println("""
                Available commands:
                   help      - Show this help
                   register  - Create a new user
                   login     - Log in as existing user
                   quit      - Exit the client
                """);
    }

    private void printPostLoginHelp() {
        System.out.println("""
                Available commands:
                   help         - Show this help
                   logout       - Log out
                   create game  - Create a new game
                   list games   - List all games
                   play game    - Join a game as a player
                   observe game - Observe a game
                """);
    }

    private void handleRegister() {
        try {
            System.out.print("Enter username: ");
            String user = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();
            System.out.print("Enter email: ");
            String email = scanner.nextLine().trim();
            RegisterResult result = server.register(user, password, email);
            if (result.authToken() != null) {
                authToken = result.authToken();
                username = user;
                System.out.println("Registration successful! You are now logged in.");
            } else {
                System.out.println("Registration failed.");
            }
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    private void handleLogin() {
        try {
            System.out.print("Enter username: ");
            String user = scanner.nextLine().trim();
            System.out.print("Enter password: ");
            String password = scanner.nextLine().trim();
            LoginResult result = server.login(user, password);
            if (result.authToken() != null) {
                authToken = result.authToken();
                username = user;
                System.out.println("Login successful!");
            } else {
                System.out.println("Login failed.");
            }
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            server.logout(authToken);
            authToken = null;
            username = null;
            System.out.println("Logged out.");
        } catch (Exception e) {
            System.out.println("Logout failed: " + e.getMessage());
        }
    }

    private void handleCreateGame() {
        try {
            System.out.print("Enter a name for the new game: ");
            String gameName = scanner.nextLine().trim();
            CreateGameResult result = server.createGame(authToken, gameName);
            if (result.gameID() > 0) {
                System.out.println("Game created: " + gameName);
            } else {
                System.out.println("Create game failed.");
            }
        } catch (Exception e) {
            System.out.println("Create game failed: " + e.getMessage());
        }
    }

    private void handleListGames() {
        try {
            lastListedGames = server.listGames(authToken);
            if (lastListedGames.isEmpty()) {
                System.out.println("No games found.");
            } else {
                System.out.println("Games:");
                int index = 1;
                for (GameInfo gameData : lastListedGames) {
                    String white = gameData.whiteUsername() != null ? gameData.whiteUsername() : "(empty)";
                    String black = gameData.blackUsername() != null ? gameData.blackUsername() : "(empty)";
                    System.out.printf("  %d.  \"%s\" | White: %s | Black: %s%n", index++, gameData.gameName(), white, black);
                }
            }
        } catch (Exception e) {
            System.out.println("List games failed: " + e.getMessage());
        }
    }

    private void handlePlayGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("No games listed. Use 'list games' first.");
                return;
            }
            System.out.print("Enter game number: ");
            int gameID = Integer.parseInt(scanner.nextLine().trim());
            if (gameID < 1 || gameID > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }
            GameInfo gameData = lastListedGames.get(gameID - 1);
            System.out.print("Enter color (white/black): ");
            String color = scanner.nextLine().trim().toLowerCase();
            if (!color.equals("white") && !color.equals("black")) {
                System.out.println("Invalid color.");
                return;
            }
            server.joinGame(authToken, gameData.gameID(), color);
            System.out.println("Joined game as " + color + "!");

            //connect websocket
            connectWebSocket(gameData.gameID());

            //Draw board
            ChessBoard board = new ChessBoard();
            board.resetBoard();
            ChessBoardPrinter.printBoard(board, color.equals("white"));

            //enter gameplay loop
            startGameplayLoop(gameData.gameID(), color);

        } catch (NumberFormatException e) {
            System.out.println("Invalid input (not a number).");
        } catch (Exception e) {
            System.out.println("Play game failed: " + e.getMessage());
        }
    }

    private void handleObserveGame() {
        try {
            if (lastListedGames.isEmpty()) {
                System.out.println("No games listed. Use 'list games' first.");
                return;
            }
            System.out.print("Enter game number: ");
            int gameID = Integer.parseInt(scanner.nextLine().trim());
            if (gameID < 1 || gameID > lastListedGames.size()) {
                System.out.println("Invalid game number.");
                return;
            }
            GameInfo gameData = lastListedGames.get(gameID - 1);
            System.out.println("Observing game...");

            //connect websocket without joining as a player
            connectWebSocket(gameData.gameID());

            //print board with white observer perspective
            ChessBoard board = new ChessBoard();
            board.resetBoard();
            ChessBoardPrinter.printBoard(board, true);

            //enter observer loop - null = observer
            startGameplayLoop(gameData.gameID(), null);

        } catch (NumberFormatException e) {
            System.out.println("Invalid input (not a number).");
        } catch (Exception e) {
            System.out.println("Observe game failed: " + e.getMessage());
        }
    }

    private void connectWebSocket(int gameID) throws Exception {
        String wsUrl = "ws://localhost:8080/ws"; // Use your server's WebSocket URL
        webSocketClient = new WebSocketClient(wsUrl);

        // Send CONNECT command
        UserGameCommand connectCmd = new UserGameCommand(
                UserGameCommand.CommandType.CONNECT, authToken, gameID);
        webSocketClient.sendCommand(connectCmd);
    }

    private void startGameplayLoop(int gameID, String playerColor) {
        System.out.println("\n[IN-GAME] Type 'help' for commands");
        while (true) {
            System.out.print("[IN-GAME] >>> ");
            String input = scanner.nextLine().trim().toLowerCase();
            switch (input) {
                case "help" -> printGameplayHelp(playerColor != null);
                case "redraw" -> redrawBoard(playerColor);
                case "leave" -> {
                    sendLeaveCommand(gameID);
                    return; // Exit gameplay loop
                }
                case "move" -> {
                    if (playerColor == null) {
                        System.out.println("Observers cannot make moves!");
                        break;
                    }
                    handleMakeMove(gameID);
                }
                case "resign" -> {
                    if (playerColor == null) {
                        System.out.println("Observers cannot resign!");
                        break;
                    }
                    sendResignCommand(gameID);
                    return; // Game over
                }
                case "highlight" -> highlightLegalMoves();
                default -> System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private void sendLeaveCommand(int gameID) {
        UserGameCommand leaveCmd = new UserGameCommand(
                UserGameCommand.CommandType.LEAVE, authToken, gameID);
        webSocketClient.sendCommand(leaveCmd);
        System.out.println("Left the game.");
    }

    private void sendResignCommand(int gameID) {
        UserGameCommand resignCmd = new UserGameCommand(
                UserGameCommand.CommandType.RESIGN, authToken, gameID);
        webSocketClient.sendCommand(resignCmd);
        System.out.println("Resigned from the game.");
    }

    private void handleMakeMove(int gameID) {
        System.out.print("Enter move (e.g., 'e2 e4'): ");
        String[] parts = scanner.nextLine().trim().split(" ");
        if (parts.length != 2) {
            System.out.println("Invalid move format. Example: 'e2 e4'");
            return;
        }
        String moveFrom = parts[0];
        String moveTo = parts[1];

        UserGameCommand moveCmd = new UserGameCommand(
                UserGameCommand.CommandType.MAKE_MOVE,
                authToken,
                gameID,
                moveFrom,
                moveTo
        );
        webSocketClient.sendCommand(moveCmd);
    }

    private void printGameplayHelp(boolean isPlayer) {
        System.out.println("""
        Available commands:
          help      - Show this help
          redraw    - Redraw the chess board
          leave     - Leave the game
          move      - Make a move (players only)
          resign    - Resign from the game (players only)
          highlight - Highlight legal moves for a piece
        """);
    }

    private void redrawBoard(String playerColor) {
        // Fetch latest game state from server if needed
        // For simplicity, we'll assume the WebSocket sends LOAD_GAME updates
        System.out.println("(Board will update automatically on moves)");
    }

    private void highlightLegalMoves() {
        //System.out.println("Highlighting legal moves is a local operation. Please select a piece on the board
        // (not implemented in this CLI).");
        // Prompt user for the square
        System.out.print("Enter the square of the piece (e.g., e2): ");
        String square = scanner.nextLine().trim().toLowerCase();

        // You need to have access to the current board state
        // For example, suppose you have a field:
        // private ChessBoard currentBoard;
        // (Update this field whenever you receive a LOAD_GAME message.)

        if (currentBoard == null) {
            System.out.println("Board state not available.");
            return;
        }

        // Convert square like "e2" to row/col indices
        int col = square.charAt(0) - 'a' + 1;
        int row = Character.getNumericValue(square.charAt(1));

        ChessPosition pos = new ChessPosition(row, col);
        ChessPiece piece = currentBoard.getPiece(pos);

        if (piece == null) {
            System.out.println("No piece at that square.");
            return;
        }

        // Get legal moves for this piece
        Collection<ChessMove> legalMoves = piece.pieceMoves(currentBoard, pos);

        if (legalMoves.isEmpty()) {
            System.out.println("No legal moves for this piece.");
            return;
        }

        System.out.println("Legal moves for " + square + ":");
        for (ChessMove move : legalMoves) {
            ChessPosition end = move.getEndPosition();
            char endCol = (char) ('a' + end.getColumn() - 1);
            System.out.println("  " + endCol + end.getRow());
        }
    }
}