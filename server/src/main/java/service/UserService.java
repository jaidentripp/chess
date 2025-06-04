package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;
import org.mindrot.jbcrypt.BCrypt;
import request.*;
import result.*;
import java.util.UUID;

public class UserService {
    private final DataAccess dao;
    public UserService(DataAccess dao) { this.dao = dao; }

    public RegisterResult register(RegisterRequest req) throws DataAccessException {
        //valid input
        if (req == null || req.username() == null || req.password() == null || req.email() == null ||
                req.username().isBlank() || req.password().isBlank() || req.email().isBlank()) {
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
        System.out.println("UserService.login called with: username=" + req.username() + ", password=" + req.password());

        if (req == null) {
            System.out.println("LoginRequest is null");
            throw new DataAccessException("Error: bad request");
        }
        if (req.username() == null) {
            System.out.println("Username is null");
            throw new DataAccessException("Error: bad request");
        }
        if (req.password() == null) {
            System.out.println("Password is null");
            throw new DataAccessException("Error: bad request");
        }
        if (req.username().isBlank()) {
            System.out.println("Username is blank");
            throw new DataAccessException("Error: bad request");
        }
        if (req.password().isBlank()) {
            System.out.println("Password is blank");
            throw new DataAccessException("Error: bad request");
        }

        UserData user = dao.getUser(req.username());
        if (user == null) {
            System.out.println("No user found for username: " + req.username());
            throw new DataAccessException("Error: unauthorized");
        } else {
            System.out.println("User found: " + user.username() + ", hashed password in DB: " + user.password());
        }

        // If you are storing hashed passwords, use BCrypt to check:
        boolean passwordMatch = BCrypt.checkpw(req.password(), user.password());
        System.out.println("BCrypt password match: " + passwordMatch);

        if (!passwordMatch) {
            System.out.println("Password does not match for user: " + req.username());
            throw new DataAccessException("Error: unauthorized");
        }

        //generate new auth token
        String authToken = UUID.randomUUID().toString();
        AuthData auth = new AuthData(authToken, req.username());
        dao.insertAuth(auth);
        System.out.println("Login successful for user: " + req.username() + ", authToken: " + authToken);
        return new LoginResult(req.username(), authToken);
    }

    public void logout(String authToken) throws DataAccessException {
        if (authToken == null || authToken.isBlank() || dao.getAuth(authToken) == null) {
            throw new DataAccessException("Error: unauthorized");
        }
        dao.deleteAuth(authToken);
    }
}
