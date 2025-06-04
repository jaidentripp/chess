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

public class Server {

    public int run(int desiredPort) {

        Spark.port(desiredPort);
        Spark.staticFileLocation("web");

        DataAccess dao;
        try {
            DatabaseManager.createDatabase();
            dao = new MySQLDataAccess();
        } catch (DataAccessException e) {
            System.err.println("Fatal error initializing database: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
        //DataAccess dao = new MemoryDataAccess();
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
                //prefix with error
                if (!message.toLowerCase().contains("error")) {
                    message = "Error: " + message;
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
                return errorMessageLogic(e, res, gson);
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
                return errorMessage401And500Logic(e, res, gson);
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
                return errorMessage401And500Logic(e, res, gson);
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
                //always prefix with error
                if (!message.toLowerCase().contains("error")) {
                    message = "Error: " + message;
                }
                return gson.toJson(Map.of("message", message));
            } catch (Exception e) {
                res.status(500);
                return gson.toJson(Map.of("message", "Error: " + e.getMessage()));
            }
        });

        //Join game endpoint
        Spark.put("/game", (req, res) -> {
           try {
               String authToken = req.headers("authorization");
               JoinGameRequest request = gson.fromJson(req.body(), JoinGameRequest.class);
               gameService.joinGame(request, authToken);
               res.type("application/json");
               res.status(200);
               return"{}";
           } catch (com.google.gson.JsonSyntaxException e) {
               res.status(400);
               return gson.toJson(Map.of("message", "Error: bad request"));
            } catch (dataaccess.DataAccessException e) {
               String message = e.getMessage();
               if ("Error: bad request".equals(message)) {
                   res.status(400);
               } else if ("Error: unauthorized".equals(message)) {
                   res.status(401);
               } else if ("Error: already taken".equals(message)) {
                   res.status(403);
               } else {
                   res.status(500);
               }
               //prefix with error
               if (!message.toLowerCase().contains("error")) {
                   message = "Error: " + message;
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

    private String errorMessageLogic(Exception e, spark.Response res, Gson gson) {
        String message = e.getMessage();
        if ("Error: bad request".equals(message)) {
            res.status(400);
        } else if ("Error: unauthorized".equals(message)) {
            res.status(401);
        } else {
            res.status(500);
        }
        // Always prefix with "Error:" if not already present
        if (message == null || !message.toLowerCase().contains("error")) {
            message = "Error: " + message;
        }
        return gson.toJson(Map.of("message", message));
    }

    private String errorMessage401And500Logic(Exception e, spark.Response res, Gson gson) {
        String message = e.getMessage();
        if ("Error: unauthorized".equals(message)) {
            res.status(401);
        } else {
            res.status(500);
        }
        // Always prefix with "Error:" if not already present
        if (message == null || !message.toLowerCase().contains("error")) {
            message = "Error: " + message;
        }
        return gson.toJson(Map.of("message", message));
    }
}
