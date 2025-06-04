package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class ClearServiceTest {
    @Test
    public void testClearPositive() throws DataAccessException {
        DataAccess dao = new MemoryDataAccess();
        dao.insertUser(new model.UserData("user", "pass", "email"));
        assertNotNull(dao.getUser("user"));
        new ClearService(dao).clear();
        assertNull(dao.getUser("user"));
    }
}
