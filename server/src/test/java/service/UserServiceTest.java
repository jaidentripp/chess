package service;

import dataaccess.*;
import org.junit.jupiter.api.*;
import request.*;
import result.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {
    DataAccess dao;
    UserService userService;

    @BeforeEach
    public void setUp() {
        dao = new MemoryDataAccess();
        userService = new UserService(dao);
    }

    //Register
    @Test
    public void testRegisterPositive() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("user", "pass", "email");
        RegisterResult result = userService.register(request);
        assertEquals("user", result.username());
        assertNotNull(result.authToken());
        assertNotNull(dao.getUser("user"));
    }

    @Test
    public void testRegisterNegativeAlreadyTaken() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("user", "pass", "email");
        userService.register(request);
        DataAccessException exception = assertThrows(DataAccessException.class, () -> userService.register(request));
        assertEquals("Error: already taken", exception.getMessage());
    }

    //Login
    @Test
    public void testLoginPositive() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("user", "pass", "email");
        userService.register(request);
        LoginRequest loginRequest = new LoginRequest("user", "pass");
        LoginResult loginResult = userService.login(loginRequest);
        assertEquals("user", loginResult.username());
        assertNotNull(loginResult.authToken());
    }

    @Test
    public void testLoginNegativeBadPassword() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("user", "pass", "email");
        userService.register(request);
        LoginRequest loginRequest = new LoginRequest("user", "wrongpass");
        DataAccessException exception = assertThrows(DataAccessException.class, () -> userService.login(loginRequest));
        assertEquals("Error: unauthorized", exception.getMessage());
    }

    //Logout
    @Test
    public void testLogoutPositive() throws DataAccessException {
        RegisterRequest request = new RegisterRequest("user", "pass", "email");
        RegisterResult result = userService.register(request);
        userService.logout(result.authToken());
        assertNull(dao.getAuth(result.authToken()));
    }

    @Test
    public void testLogoutNegativeInvalidToken() {
        DataAccessException exception = assertThrows(DataAccessException.class, () -> userService.logout("badtoken"));
        assertEquals("Error: unauthorized", exception.getMessage());
    }
}
