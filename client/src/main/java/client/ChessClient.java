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

public class ChessClient {
    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade server;
    private String authToken = null;
    private String username = null;
    private List<GameInfo> lastListedGames = new ArrayList<>();

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
            //Draw board
            ChessBoard board = new ChessBoard();
            board.resetBoard();
            ChessBoardPrinter.printBoard(board, color.equals("white"));
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
            //print board with white observer perspective
            ChessBoard board = new ChessBoard();
            board.resetBoard();
            ChessBoardPrinter.printBoard(board, true);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input (not a number).");
        } catch (Exception e) {
            System.out.println("Observe game failed: " + e.getMessage());
        }
    }
}