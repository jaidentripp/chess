package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;
import request.*;
import result.*;
import java.util.UUID;

import javax.xml.crypto.Data;

public class UserService {
    private final DataAccess dao;
    public UserService(DataAccess dao) { this.dao = dao; }

    public RegisterResult register(RegisterRequest req) throws DataAccessException {
        //valid input
        if (req == null || req.username() == null || req.password() == null || req.email() == null || req.username().isBlank() || req.password().isBlank() || req.email().isBlank()) {
            throw new DataAccessException("Error: bad request");
        }
        //check if user already exists
        if (dao.getUser(req.username()) != null) {
            throw new DataAccessException("Error: already taken");
        }

        //create user
        UserData user = new UserData(req.username(), req.password(), req.email());
        dao.insertUser(user);

        //generate auth token
        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, req.username());
        dao.insertAuth(auth);

        return new RegisterResult(req.username(), authToken);
    }

    public LoginResult login(LoginRequest req) throws DataAccessException {
        if (req == null || req.username() == null || req.password() == null || req.username().isBlank() || req.password().isBlank()) {
            throw new DataAccessException("Error: bad request");
        }
        UserData user = dao.getUser(req.username());
        if (user == null || !user.password().equals(req.password())) {
            throw new DataAccessException("Error: unauthorized");
        }

        //generate new auth token
        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, req.username());
        dao.insertAuth(auth);
        return new LoginResult(req.username(), authToken);
    }

    public void logout(String authToken) throws DataAccessException {
        if (authToken == null || authToken.isBlank() || dao.getAuth(authToken) == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        dao.deleteAuth(authToken);
    }
}
