package client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server.Server;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ServerFacadeTests {
    private static Server server;
    private static ServerFacade serverFacade;
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test on HTTP server on " + port);
        serverFacade = new ServerFacade("http://localhost:" + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearDatabase() throws IOException {
        //clear db before each test
        URL url = new URL("http://localhost:" + port + "/db");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        assertEquals(200, conn.getResponseCode());
    }

    @Test
    void registerPositive() throws Exception {
        var result = serverFacade.register("user1", "pass1", "email@test.com");
        assertNotNull(result.authToken());
        assertTrue(result.authToken().length() > 10);
    }

    @Test
    void registerNegative() {
        Exception e = assertThrows(IOException.class, () -> {
            serverFacade.register("", "pass1", "email@test.com");
        });
        assertTrue(e.getMessage().contains("bad request"));
    }

    @Test
    void loginPositive() throws Exception {
        serverFacade.register("user1", "pass1", "email@test.com");
        var result = serverFacade.login("user1", "pass1");
        assertNotNull(result.authToken());
    }

    @Test
    void loginNegative() {
        Exception e = assertThrows(IOException.class, () -> {
            serverFacade.login("nonexistent", "wrongpass");
        });
        assertTrue(e.getMessage().contains("unauthorized"));
    }

    @Test
    void logoutPositive() throws Exception {
        var auth = serverFacade.register("user1", "pass1", "email@test.com");
        assertDoesNotThrow(() -> serverFacade.logout(auth.authToken()));
    }

    @Test
    void logoutNegative() {
        Exception e = assertThrows(IOException.class, () -> {
            serverFacade.logout("invalid_token");
        });
        assertTrue(e.getMessage().contains("unauthorized"));
    }

    @Test
    void createGamePositive() throws Exception {
        var auth = serverFacade.register("user1", "pass1", "email@test.com");
        var result = serverFacade.createGame(auth.authToken(), "testGame");
        assertTrue(result.gameID() > 0);
    }

    @Test
    void createGameNegative() {
        Exception e = assertThrows(IOException.class, () -> {
            serverFacade.createGame("invalid_token", "testGame");
        });
        assertTrue(e.getMessage().contains("unauthorized"));
    }

    @Test
    void listGamesPositive() throws Exception {
        var auth = serverFacade.register("user1", "pass1", "email@test.com");
        serverFacade.createGame(auth.authToken(), "game1");
        serverFacade.createGame(auth.authToken(), "game2");

        var games = serverFacade.listGames(auth.authToken());
        assertEquals(2, games.size());
        assertEquals("game1", games.get(0).gameName());
    }

    @Test
    void listGamesNegative() {
        Exception e = assertThrows(IOException.class, () -> {
            serverFacade.listGames("invalid_token");
        });
        assertTrue(e.getMessage().contains("unauthorized"));
    }

    @Test
    void joinGamePositive() throws Exception {
        var auth = serverFacade.register("user1", "pass1", "email@test.com");
        var game = serverFacade.createGame(auth.authToken(), "testGame");

        assertDoesNotThrow(() ->
                serverFacade.joinGame(auth.authToken(), game.gameID(), "WHITE")
        );
    }

    @Test
    void joinGameNegative() throws Exception {
        var auth = serverFacade.register("user1", "pass1", "email@test.com");
        var game = serverFacade.createGame(auth.authToken(), "testGame");

        Exception e = assertThrows(IOException.class, () -> {
            serverFacade.joinGame(auth.authToken(), 9999, "WHITE");
        });
        assertTrue(e.getMessage().contains("bad request"));
    }
}
