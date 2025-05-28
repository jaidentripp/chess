package dataaccess;

import chess.ChessGame;
import model.AuthData;
import model.GameData;
import model.UserData;
import com.google.gson.Gson;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySQLDataAccess implements DataAccess {
    private final Gson gson = new Gson();

    public MySQLDataAccess() throws DataAccessException {
        createTablesIfNotExists();
    }

    private void createTablesIfNotExists() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    username VARCHAR(255) PRIMARY KEY,
                    password_hash VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL UNIQUE
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS auths (
                    authToken VARCHAR(255) PRIMARY KEY,
                    username VARCHAR(255) NOT NULL,
                    FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
                )""");
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS games (
                    gameID INT AUTO_INCREMENT PRIMARY KEY,
                    whiteUsername VARCHAR(255),
                    blackUsername VARCHAR(255),
                    gameName VARCHAR(255) NOT NULL,
                    game TEXT NOT NULL,
                    FOREIGN KEY (whiteUsername) REFERENCES users(username) ON DELETE SET NULL,
                    FOREIGN KEY (blackUsername) REFERENCES users(username) ON DELETE SET NULL
                )""");
        } catch (SQLException ex) {
            throw new DataAccessException("Failed to create tables", ex);
        }
    }

    //Users
    @Override
    public void insertUser(UserData user) throws DataAccessException {
        String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username());
            ps.setString(2, hashedPassword);
            ps.setString(3, user.email());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate")) {
                throw new DataAccessException("Error: already taken");
            }
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

//    @Override
//    public void registerUser(String username, String password, String email) throws DataAccessException {
//        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
//        try (var conn = DatabaseManager.getConnection();
//             var statement = conn.prepareStatement("INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)")) {
//            statement.setString(1, username);
//            statement.setString(2, hashedPassword);
//            statement.setString(3, email);
//            statement.executeUpdate();
//        } catch (SQLException e) {
//            if (e.getMessage().contains("Duplicate")) {
//                throw new DataAccessException("Error: already taken");
//            }
//            throw new DataAccessException("Error: " + e.getMessage());
//        }
//    }

    @Override
    public boolean verifyUser(String username, String password) throws DataAccessException {
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try(var conn = DatabaseManager.getConnection();
            var statement = conn.prepareStatement(sql)) {
            statement.setString(1, username);
            var rs = statement.executeQuery();
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    return BCrypt.checkpw(password, hash);
                }
                return false;
        } catch (SQLException e) {
            throw new DataAccessException("Error verifying user", e);
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        String sql = "SELECT * FROM users WHERE username=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return new UserData(
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("email")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    //Games
    @Override
    public void insertGame(GameData game) throws DataAccessException {
        String sql = "INSERT INTO games (whiteUsername, blackUsername, gameName, game) VALUES (?, ?, ?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, game.whiteUsername());
            ps.setString(2, game.blackUsername());
            ps.setString(3, game.gameName());
            ps.setString(4, gson.toJson(game.game()));
            ps.executeUpdate();

            //set auto-generated gameID if needed
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int newGameID = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {
        String sql = "SELECT * FROM games WHERE gameID=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameID);
            var rs = ps.executeQuery();
            if (rs.next()) {
                ChessGame chessGame = gson.fromJson(rs.getString("game"), ChessGame.class);
                return new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        chessGame
                );
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {
        String sql = "SELECT * FROM games";
        List<GameData> games = new ArrayList<>();
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            while (rs.next()) {
                ChessGame chessGame = gson.fromJson(rs.getString("game"), ChessGame.class);
                games.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        chessGame
                ));
            }
            return games;
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    @Override public void updateGame(GameData game) throws DataAccessException {
        String sql = "UPDATE games SET whiteUsername=?, blackUsername=?, gameName=?, game=? WHERE gameID=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, game.whiteUsername());
            ps.setString(2, game.blackUsername());
            ps.setString(3, game.gameName());
            ps.setString(4, gson.toJson(game.game()));
            ps.setInt(5, game.gameID());
            int updated = ps.executeUpdate();
            if (updated == 0) {
                throw new DataAccessException("Game does not exist.");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    //Auths
    @Override
    public void insertAuth(AuthData auth) throws DataAccessException {
        String sql = "INSERT INTO auths (authToken, username) VALUES (?, ?)";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, auth.authToken());
            ps.setString(2, auth.username());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT * FROM auths WHERE authToken=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            var rs = ps.executeQuery();
            if (rs.next()) {
                return new AuthData(
                        rs.getString("authToken"),
                        rs.getString("username")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auths WHERE authToken=?";
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            ps.setString(1, authToken);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    //Clear all data
    @Override
    public void clear() throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM auths");
            stmt.executeUpdate("DELETE FROM games");
            stmt.executeUpdate("DELETE FROM users");
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }

    //utility for executing updates
    private void executeUpdate(String sql, Object... params) throws DataAccessException {
        try (var conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Error: " + e.getMessage());
        }
    }
}
