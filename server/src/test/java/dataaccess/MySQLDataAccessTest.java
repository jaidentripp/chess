package dataaccess;

import model.UserData;
import model.GameData;
import model.AuthData;
import chess.ChessGame;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLDataAccessTest {
    private MySQLDataAccess dao;

    @BeforeEach
    void setup() throws DataAccessException {
        dao = new MySQLDataAccess();
        dao.clear(); // Reset DB before each test
    }

    //User tests

    @Test
    void insertUserPositive() throws DataAccessException {
        UserData user = new UserData("user1", "password", "user1@email.com");
        dao.insertUser(user);

        UserData fromDb = dao.getUser("user1");
        assertNotNull(fromDb);
        assertEquals("user1", fromDb.username());
        assertEquals("user1@email.com", fromDb.email());
        // Password should NOT be stored in plaintext
        assertNotEquals("password", fromDb.password());
    }

    @Test
    void insertUserNegativeDuplicate() throws DataAccessException {
        UserData user = new UserData("user1", "password", "user1@email.com");
        dao.insertUser(user);
        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            dao.insertUser(user);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("already taken") || ex.getMessage().toLowerCase().contains("duplicate"));
    }

    @Test
    void getUserNegativeNotFound() throws DataAccessException {
        assertNull(dao.getUser("noSuchUser"));
    }

    @Test
    void verifyUserPositive() throws DataAccessException {
        UserData user = new UserData("user2", "secret", "user2@email.com");
        dao.insertUser(user);
        assertTrue(dao.verifyUser("user2", "secret"));
    }

    @Test
    void verifyUserNegativeWrongPassword() throws DataAccessException {
        UserData user = new UserData("user3", "secret", "user3@email.com");
        dao.insertUser(user);
        assertFalse(dao.verifyUser("user3", "wrongpassword"));
    }

    @Test
    void verifyUserNegativeNoSuchUser() throws DataAccessException {
        assertFalse(dao.verifyUser("noUser", "pw"));
    }

    //Auth tests

    @Test
    void insertAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token1", "user1");
        dao.insertAuth(auth);
        AuthData fromDb = dao.getAuth("token1");
        assertNotNull(fromDb);
        assertEquals("user1", fromDb.username());
    }

    @Test
    void insertAuthNegativeDuplicate() throws DataAccessException {
        AuthData auth = new AuthData("token2", "user2");
        dao.insertAuth(auth);
        DataAccessException ex = assertThrows(DataAccessException.class, () -> {
            dao.insertAuth(auth);
        });
        assertTrue(ex.getMessage().toLowerCase().contains("duplicate"));
    }

    @Test
    void getAuthNegativeNotFound() throws DataAccessException {
        assertNull(dao.getAuth("noToken"));
    }

    @Test
    void deleteAuthPositive() throws DataAccessException {
        AuthData auth = new AuthData("token3", "user3");
        dao.insertAuth(auth);
        dao.deleteAuth("token3");
        assertNull(dao.getAuth("token3"));
    }

    @Test
    void deleteAuthNegativeNotFound() throws DataAccessException {
        //Should not throw, but nothing to delete
        assertDoesNotThrow(() -> dao.deleteAuth("noSuchToken"));
    }

    //Game tests

    @Test
    void insertGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, "white", "black", "testGame", game);
        int gameId = dao.insertGame(gameData);
        GameData fromDb = dao.getGame(gameId);
        assertNotNull(fromDb);
        assertEquals("testGame", fromDb.gameName());
        assertEquals("white", fromDb.whiteUsername());
    }

    @Test
    void insertGameNegativeNullName() {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, "white", "black", null, game);
        assertThrows(DataAccessException.class, () -> dao.insertGame(gameData));
    }

    @Test
    void getGameNegativeNotFound() throws DataAccessException {
        assertNull(dao.getGame(9999));
    }

    @Test
    void updateGamePositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(0, "white", "black", "gameName", game);
        int gameId = dao.insertGame(gameData);

        GameData updated = new GameData(gameId, "white", "black", "newName", game);
        dao.updateGame(updated);

        GameData fromDb = dao.getGame(gameId);
        assertEquals("newName", fromDb.gameName());
    }

    @Test
    void updateGameNegativeNotFound() {
        ChessGame game = new ChessGame();
        GameData gameData = new GameData(12345, "white", "black", "gameName", game);
        assertThrows(DataAccessException.class, () -> dao.updateGame(gameData));
    }

    @Test
    void listGamesPositive() throws DataAccessException {
        ChessGame game = new ChessGame();
        GameData gameData1 = new GameData(0, "white", "black", "game1", game);
        GameData gameData2 = new GameData(0, "white", "black", "game2", game);
        dao.insertGame(gameData1);
        dao.insertGame(gameData2);
        List<GameData> games = dao.listGames();
        assertTrue(games.size() >= 2);
        assertTrue(games.stream().anyMatch(g -> g.gameName().equals("game1")));
        assertTrue(games.stream().anyMatch(g -> g.gameName().equals("game2")));
    }

    //Clear tests

    @Test
    void clearPositive() throws DataAccessException {
        UserData user = new UserData("userX", "pw", "userX@email.com");
        dao.insertUser(user);
        assertNotNull(dao.getUser("userX"));
        dao.clear();
        assertNull(dao.getUser("userX"));
    }
}
