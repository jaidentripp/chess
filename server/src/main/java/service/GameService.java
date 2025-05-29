package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;
import request.*;
import result.*;
import java.util.*;

public class GameService {
    private final DataAccess dao;
    private static int nextGameId = 1;

    public GameService(DataAccess dao) { this.dao = dao; }

    public CreateGameResult createGameResult(CreateGameRequest req, String authToken) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        if (req == null || req.gameName() == null || req.gameName().isBlank()) {
            throw new DataAccessException("Error: bad request");
        }
        //int gameID = nextGameId++;

        GameData game = new GameData( 0, null, null, req.gameName(), new chess.ChessGame());
        int gameID = dao.insertGame(game);
        return new CreateGameResult(gameID);
    }

    public ListGamesResult listGames(String authToken) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        List<GameData> games = dao.listGames();
        return new ListGamesResult(games);
    }

    public void joinGame(JoinGameRequest req, String authToken) throws DataAccessException {
        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        GameData game = dao.getGame(req.gameID());
        if (game == null) {
            throw new DataAccessException("Error: bad request");
        }

        //assign player to color
        if ("WHITE".equalsIgnoreCase(req.playerColor())) {
            if (game.whiteUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            game = new GameData(game.gameID(), auth.username(), game.blackUsername(), game.gameName(), game.game());
        } else if ("BLACK".equalsIgnoreCase(req.playerColor())) {
            if (game.blackUsername() != null) {
                throw new DataAccessException("Error: already taken");
            }
            game = new GameData(game.gameID(), game.whiteUsername(), auth.username(), game.gameName(), game.game());
        } else {
            throw new DataAccessException("Error: bad request");
        }
        dao.updateGame(game);
    }
}
