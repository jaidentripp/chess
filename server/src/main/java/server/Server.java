package server;

import com.google.gson.JsonSyntaxException;
import dataaccess.DatabaseManager;
import request.LoginRequest;
import request.RegisterRequest;
import result.ListGamesResult;
import result.LoginResult;
import result.RegisterResult;
import service.UserService;
import spark.*;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import service.ClearService;
import dataaccess.DataAccessException;

import java.util.Map;

import service.GameService;
import request.CreateGameRequest;
import result.CreateGameResult;
import request.JoinGameRequest;

import dataaccess.MySQLDataAccess;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter;
import static spark.Spark.*;

public class Server {

    public int run(int desiredPort) {

        Spark.webSocket("/ws", websocket.WebSocketServer.class);

        Spark.port(desiredPort);
        Spark.staticFileLocation("web");

        DataAccess dao = initializeDatabase();
        Gson gson = new Gson();

        ClearService clearService = new ClearService(dao);
        UserService userService = new UserService(dao);
        GameService gameService = new GameService(dao);

        Spark.delete("/db", (req, res) -> handleClear(clearService, req, res, gson));
        Spark.post("/user", (req, res) -> handleRegister(userService, req, res, gson));
        Spark.post("/session", (req, res) -> handleLogin(userService, req, res, gson));
        Spark.delete("/session", (req, res) -> handleLogout(userService, req, res, gson));
        Spark.get("/game", (req, res) -> handleListGames(gameService, req, res, gson));
        Spark.post("/game", (req, res) -> handleCreateGame(gameService, req, res, gson));
        Spark.put("/game", (req, res) -> handleJoinGame(gameService, req, res, gson));

        Spark.init();

        Spark.awaitInitialization();

        System.out.println("Server started on port: " + Spark.port());

        return Spark.port();
    }

    private DataAccess initializeDatabase() {
        try {
            DatabaseManager.createDatabase();
            return new MySQLDataAccess();
        } catch (DataAccessException e) {
            System.err.println("Fatal error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private Object handleClear(ClearService clearService, Request req, Response res, Gson gson) {
        try {
            clearService.clear();
            res.status(200);
            res.type("application/json");
            return "{}";
        } catch (DataAccessException e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private Object handleRegister(UserService userService, Request req, Response res, Gson gson) {
        try {
            RegisterRequest request = gson.fromJson(req.body(), RegisterRequest.class);
            RegisterResult result = userService.register(request);
            res.type("application/json");
            res.status(200);
            return gson.toJson(result);
        } catch (JsonSyntaxException e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res, gson);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private Object handleLogin(UserService userService, Request req, Response res, Gson gson) {
        try {
            LoginRequest request = gson.fromJson(req.body(), LoginRequest.class);
            LoginResult result = userService.login(request);
            res.type("application/json");
            res.status(200);
            return gson.toJson(result);
        } catch (JsonSyntaxException e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res, gson);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private Object handleLogout(UserService userService, Request req, Response res, Gson gson) {
        try {
            String authToken = req.headers("authorization");
            userService.logout(authToken);
            res.status(200);
            res.type("application/json");
            return "{}";
        } catch (DataAccessException e) {
            return handleUnauthorizedOrServerError(e, res, gson);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private Object handleListGames(GameService gameService, Request req, Response res, Gson gson) {
        try {
            String authToken = req.headers("authorization");
            ListGamesResult result = gameService.listGames(authToken);
            res.type("application/json");
            res.status(200);
            return gson.toJson(result);
        } catch (DataAccessException e) {
            return handleUnauthorizedOrServerError(e, res, gson);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private Object handleCreateGame(GameService gameService, Request req, Response res, Gson gson) {
        try {
            String authToken = req.headers("authorization");
            CreateGameRequest request = gson.fromJson(req.body(), CreateGameRequest.class);
            CreateGameResult result = gameService.createGameResult(request, authToken);
            res.type("application/json");
            res.status(200);
            return gson.toJson(result);
        } catch (JsonSyntaxException e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res, gson);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private Object handleJoinGame(GameService gameService, Request req, Response res, Gson gson) {
        try {
            String authToken = req.headers("authorization");
            JoinGameRequest request = gson.fromJson(req.body(), JoinGameRequest.class);
            gameService.joinGame(request, authToken);
            res.type("application/json");
            res.status(200);
            return "{}";
        } catch (JsonSyntaxException e) {
            res.status(400);
            return gson.toJson(Map.of("message", "Error: bad request"));
        } catch (DataAccessException e) {
            return handleDataAccessException(e, res, gson);
        } catch (Exception e) {
            res.status(500);
            return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
        }
    }

    private String handleDataAccessException(DataAccessException e, Response res, Gson gson) {
        String message = e.getMessage();
        int status;
        if ("Error: bad request".equals(message)) {
            status = 400;
        } else if ("Error: unauthorized".equals(message)) {
            status = 401;
        } else if ("Error: already taken".equals(message)) {
            status = 403;
        } else {
            status = 500;
        }
        if (message == null || !message.toLowerCase().contains("error")) {
            message = "Error: " + message;
        }
        res.status(status);
        return gson.toJson(Map.of("message", message));
    }

    private String handleUnauthorizedOrServerError(DataAccessException e, Response res, Gson gson) {
        String message = e.getMessage();
        int status = "Error: unauthorized".equals(message) ? 401 : 500;
        if (message == null || !message.toLowerCase().contains("error")) {
            message = "Error: " + message;
        }
        res.status(status);
        return gson.toJson(Map.of("message", message));
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
