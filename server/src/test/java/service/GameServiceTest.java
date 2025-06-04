package service;

import dataaccess.*;
import model.*;
import org.junit.jupiter.api.*;
import request.*;
import result.*;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {
    DataAccess dao;
    UserService userService;
    GameService gameService;
    String authToken;

    @BeforeEach
    public void setup() throws DataAccessException {
        dao = new MemoryDataAccess();
        userService = new UserService(dao);
        gameService = new GameService(dao);
        RegisterResult registerResult = userService.register(new RegisterRequest("user", "pass",
                "email"));
        authToken = registerResult.authToken();
    }

    //Create game
    @Test
    public void testCreateGamePositive() throws DataAccessException {
        CreateGameRequest request = new CreateGameRequest("MyGame");
        CreateGameResult result = gameService.createGameResult(request, authToken);
        assertTrue(result.gameID() > 0);
        assertNotNull(dao.getGame(result.gameID()));
    }

    @Test
    public void testCreateGameNegativeBadRequest() {
        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                gameService.createGameResult(new CreateGameRequest(""), authToken));
        assertEquals("Error: bad request", exception.getMessage());
    }

    //list game
    @Test
    public void testListGamesPositive() throws DataAccessException {
        CreateGameResult result = gameService.createGameResult(new CreateGameRequest("Game1"), authToken);
        ListGamesResult listGamesResult = gameService.listGames(authToken);
        assertFalse(listGamesResult.games().isEmpty());
        assertEquals("Game1", listGamesResult.games().get(0).gameName());
    }

    @Test
    public void testListGamesNegativeUnauthorized() {
        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                gameService.listGames("badtoken"));
        assertEquals("Error: unauthorized", exception.getMessage());
    }

    //join game
    @Test
    public void testJoinGamePositive() throws DataAccessException {
        CreateGameResult result = gameService.createGameResult(new CreateGameRequest("Game1"), authToken);
        JoinGameRequest joinGameRequest = new JoinGameRequest("BLACK", result.gameID());
        gameService.joinGame(joinGameRequest, authToken);
        GameData game = dao.getGame(result.gameID());
        assertEquals("user", game.blackUsername());
    }

    @Test
    public void testJoinGameNegativeAlreadyTaken() throws DataAccessException {
        CreateGameResult result = gameService.createGameResult(new CreateGameRequest("Game1"), authToken);
        JoinGameRequest joinGameRequest = new JoinGameRequest("BLACK", result.gameID());
        gameService.joinGame(joinGameRequest, authToken);
        //register another user and try to claim black again
        RegisterResult result2 = userService.register(new RegisterRequest("user2", "pass2",
                "email2"));
        JoinGameRequest joinGameRequest2 = new JoinGameRequest("BLACK", result.gameID());
        DataAccessException exception = assertThrows(DataAccessException.class, () ->
                gameService.joinGame(joinGameRequest2, result2.authToken()));
        assertEquals("Error: already taken", exception.getMessage());
    }
}
