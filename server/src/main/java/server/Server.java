package server;

import spark.*;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import service.ClearService;
import dataaccess.DataAccessException;
import java.util.Map;

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

        //Register endpoints
        Spark.delete("/db", (req, res) -> {
            try {
                new ClearService(dao).clear();
                res.status(200);
                res.type("application/json");
                return "{}";
            } catch (DataAccessException e) {
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
