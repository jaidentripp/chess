package client;

import java.util.List;

public class ListGamesResult {
    private List<GameInfo> games;
    private String message;

    public List<GameInfo> games() {
        return games;
    }
    public String message() {
        return message;
    }
}
