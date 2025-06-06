package dataaccess;

import model.AuthData;
import model.GameData;
import model.UserData;

import java.util.List;

public interface DataAccess {
    void clear() throws DataAccessException;
    void insertUser(UserData user) throws DataAccessException;
    UserData getUser(String username) throws DataAccessException;
    boolean verifyUser(String username, String password) throws DataAccessException;
    int insertGame(GameData game) throws DataAccessException;
    GameData getGame(int gameID) throws DataAccessException;
    List<GameData> listGames() throws DataAccessException;
    void updateGame(GameData game) throws DataAccessException;
    void insertAuth(AuthData auth) throws DataAccessException;
    AuthData getAuth(String authToken) throws DataAccessException;
    void deleteAuth(String authToken) throws DataAccessException;
}
