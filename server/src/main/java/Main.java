import chess.*;
import server.Server;

public class Main {
    public static void main(String[] args) {
        var piece = new ChessPiece(ChessGame.TeamColor.WHITE, ChessPiece.PieceType.PAWN);
        System.out.println("♕ 240 Chess Server: " + piece);

        // Create and start the server on port 8080
        Server server = new Server();
        int actualPort = server.run(8080);
        System.out.println("Server started on port: " + actualPort);
    }
}