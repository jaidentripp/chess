package client;

import com.google.gson.Gson;
import model.GameData;
import request.CreateGameRequest;
import request.JoinGameRequest;
import request.LoginRequest;
import request.RegisterRequest;
import result.CreateGameResult;
import result.ListGamesResult;
import result.LoginResult;
import result.RegisterResult;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public RegisterResult register(String username, String password, String email) throws IOException {
        RegisterRequest req = new RegisterRequest(username, password, email);
        String json = gson.toJson(req);
        String response = post("/user", json, null);
        return gson.fromJson(response, RegisterResult.class);
    }

    public LoginResult login(String username, String password) throws IOException {
        LoginRequest req = new LoginRequest(username, password);
        String json = gson.toJson(req);
        String response = post("/session", json, null);
        return gson.fromJson(response, LoginResult.class);
    }

    public void logout(String authToken) throws IOException {
        delete("/session", authToken);
    }

    public List<GameData> listGames(String authToken) throws IOException {
        String response = get("/game", authToken);
        ListGamesResult result = gson.fromJson(response, ListGamesResult.class);
        return result.games();
    }

    public CreateGameResult createGame(String authToken, String gameName) throws IOException {
        CreateGameRequest req = new CreateGameRequest(gameName);
        String json = gson.toJson(req);
        String response = post("/game", json, authToken);
        return gson.fromJson(response, CreateGameResult.class);
    }

    public void joinGame(String authToken, int gameID, String color) throws IOException {
        JoinGameRequest req = new JoinGameRequest(color, gameID);
        String json = gson.toJson(req);
        put("/game", json, authToken);
    }

    //http helper methods

    private String get(String path, String authToken) throws IOException {
        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        if (authToken != null) {
            conn.setRequestProperty("authorization", authToken);
        }
        return readResponse(conn);
    }

    private String post(String path, String body, String authToken) throws IOException {
        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        if (authToken != null) {
            conn.setRequestProperty("authorization", authToken);
        }
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }
        return readResponse(conn);
    }

    private String put(String path, String body, String authToken) throws IOException {
        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        if (authToken != null) {
            conn.setRequestProperty("authorization", authToken);
        }
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes());
        }
        return readResponse(conn);
    }

    private String delete(String path, String authToken) throws IOException {
        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        if (authToken != null) {
            conn.setRequestProperty("authorization", authToken);
        }
        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream inputStream = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            if (status >= 400) {
                Map<Object, String> error = gson.fromJson(stringBuilder.toString(), Map.class);
                String message = (String) error.getOrDefault("message", "Unknown error");
                throw new IOException(message);
            }
            return stringBuilder.toString();
        }
    }
}
