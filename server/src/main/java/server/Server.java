package server;

import com.google.gson.JsonSyntaxException;
import request.LoginRequest;
import request.RegisterRequest;
import result.ListGamesResult;
import result.LoginResult;
import result.RegisterResult;
import service.UserService;
import spark.*;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import dataaccess.DataAccessException;
import java.util.Map;

import service.GameService;
import result.ListGamesResult;
import request.CreateGameRequest;
import result.CreateGameResult;

public class Server {

//    public int run(int desiredPort) {
//        Spark.port(desiredPort);
//
//        Spark.staticFiles.location("web");
//
//        // Register your endpoints and handle exceptions here.
//
//        //This line initializes the server and can be removed once you have a functioning endpoint
//        Spark.init();
//
//        Spark.awaitInitialization();
//        return Spark.port();
//    }

    public int run(int desiredPort) {
        Spark.port(desiredPort);
        Spark.staticFileLocation("web");

        DataAccess dao = new MemoryDataAccess();
        Gson gson = new Gson();
        ClearService clearService = new ClearService(dao);
        UserService userService = new UserService(dao);
        GameService gameService = new GameService(dao);

        //Clear endpoint
        Spark.delete("/db", (req, res) -> {
            try {
                clearService.clear();
                res.status(200);
                res.type("application/json");
                return "{}";
            } catch (DataAccessException e) {
                res.status(500);
                return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        //Register endpoint
        Spark.post("/user", (req, res) -> {
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
                String message = e.getMessage();
                switch (message) {
                    case "Error: bad request" -> res.status(400);
                    case "Error: already taken" -> res.status(403);
                    default -> res.status(500);
                }
                return gson.toJson(Map.of("message", message));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        //Login endpoint
        Spark.post("/session", (req, res) -> {
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
                res.status(e.getMessage().equals("Error: unauthorized") ? 401 : 500);
                return gson.toJson(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        //Logout endpoint
        Spark.delete("/session", (req ,res) -> {
            try {
                String authToken = req.headers("authorization");
                userService.logout(authToken);
                res.status(200);
                res.type("application/json");
                return "{}";
            } catch (DataAccessException e) {
                res.status(401);
                return gson.toJson(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        //List games endpoint
        Spark.get("/game", (req, res) -> {
            try {
                String authToken = req.headers("authorization");
                ListGamesResult result = gameService.listGames(authToken);
                res.type("application/json");
                res.status(200);
                return gson.toJson(result);
            } catch (DataAccessException e) {
                res.status(401);
                return gson.toJson(Map.of("message", e.getMessage()));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        //Create game endpoint
        Spark.post("/game", (req, res) -> {
            try {
                String authToken = req.headers("authorization");
                CreateGameRequest request = gson.fromJson(req.body(), CreateGameRequest.class);
                CreateGameResult result = gameService.createGameResult(request, authToken);
                res.type("application/json");
                res.status(200);
                return gson.toJson(result);
            } catch (com.google.gson.JsonSyntaxException e) {
                res.status(400);
                return gson.toJson(Map.of("message", "Error: bad request"));
            } catch (dataaccess.DataAccessException e) {
                String message = e.getMessage();
                if ("Error: bad request".equals(message)) {
                    res.status(400);
                } else if ("Error: unauthorized".equals(message)) {
                    res.status(401);
                } else {
                    res.status(500);
                }
                return gson.toJson(Map.of("message", message));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        Spark.awaitInitialization();
        return Spark.port();
    }

    public void stop() {
        Spark.stop();
        Spark.awaitStop();
    }
}
