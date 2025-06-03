import chess.*;

import client.ChessClient;

public class Main {
    public static void main(String[] args) {
        //var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        //System.out.println("♕ 240 Chess Client: " + piece);
        String serverUrl = "http://localhost:8080"; // Change if needed
        new ChessClient(serverUrl).run();
    }
}