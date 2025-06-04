package client;

import org.junit.jupiter.api.*;
import server.Server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade serverFacade;
    private static int port;

    @BeforeAll
    public static void init() {
        server = new Server();
        port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        serverFacade = new ServerFacade("http://localhost:" + port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }


    @BeforeEach
    void clearDatabase() throws IOException {
        // Clear database before each test
        URL url = new URL("http://localhost:" + port + "/db");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");
        assertEquals(200, connection.getResponseCode());
    }

    // --- Register ---

    @Test
    void registerPositive() throws Exception {
        var result = serverFacade.register("user1", "pass1", "user1@email.com");
        assertNotNull(result.authToken());
        assertTrue(result.authToken().length() > 10);
    }

    @Test
    void registerNegative() {
        Exception ex = assertThrows(IOException.class, () ->
                serverFacade.register("", "pass1", "user1@email.com") // Empty username
        );
        assertTrue(ex.getMessage().toLowerCase().contains("bad request"));
    }

    // --- Login ---

    @Test
    void loginPositive() throws Exception {
        serverFacade.register("user2", "pass2", "user2@email.com");
        var result = serverFacade.login("user2", "pass2");
        assertNotNull(result.authToken());
    }

    @Test
    void loginNegative() {
        Exception ex = assertThrows(IOException.class, () ->
                serverFacade.login("nonexistent", "wrongpass")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    // --- Logout ---

    @Test
    void logoutPositive() throws Exception {
        var reg = serverFacade.register("user3", "pass3", "user3@email.com");
        assertDoesNotThrow(() -> serverFacade.logout(reg.authToken()));
    }

    @Test
    void logoutNegative() {
        Exception ex = assertThrows(IOException.class, () ->
                serverFacade.logout("invalid_token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    // --- Create Game ---

    @Test
    void createGamePositive() throws Exception {
        var reg = serverFacade.register("user4", "pass4", "user4@email.com");
        var result = serverFacade.createGame(reg.authToken(), "gameA");
        assertTrue(result.gameID() > 0);
    }

    @Test
    void createGameNegative() {
        Exception ex = assertThrows(IOException.class, () ->
                serverFacade.createGame("invalid_token", "gameB")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    // --- List Games ---

    @Test
    void listGamesPositive() throws Exception {
        var reg = serverFacade.register("user5", "pass5", "user5@email.com");
        serverFacade.createGame(reg.authToken(), "game1");
        serverFacade.createGame(reg.authToken(), "game2");
        List<GameInfo> games = serverFacade.listGames(reg.authToken());
        assertEquals(2, games.size());
        assertEquals("game1", games.get(0).gameName());
        assertEquals("game2", games.get(1).gameName());
    }

    @Test
    void listGamesNegative() {
        Exception ex = assertThrows(IOException.class, () ->
                serverFacade.listGames("invalid_token")
        );
        assertTrue(ex.getMessage().toLowerCase().contains("unauthorized"));
    }

    // --- Join Game ---

    @Test
    void joinGamePositive() throws Exception {
        var reg = serverFacade.register("user6", "pass6", "user6@email.com");
        var game = serverFacade.createGame(reg.authToken(), "gameX");
        assertDoesNotThrow(() -> serverFacade.joinGame(reg.authToken(), game.gameID(), "WHITE"));
    }

    @Test
    void joinGameNegative() throws Exception {
        var reg = serverFacade.register("user7", "pass7", "user7@email.com");
        var game = serverFacade.createGame(reg.authToken(), "gameY");
        Exception ex = assertThrows(IOException.class, () ->
                serverFacade.joinGame(reg.authToken(), 99999, "WHITE") // Invalid game ID
        );
        assertTrue(ex.getMessage().toLowerCase().contains("bad request"));
    }

}
