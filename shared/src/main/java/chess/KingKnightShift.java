package chess;

import java.util.Collection;

public class KingKnightShift {
    public static void shift(
            ChessBoard board,
            ChessPosition myPosition,
            Collection<ChessMove> moves,
            int cDirection,
            int rDirection,
            ChessGame.TeamColor color
    ) {
        int c = myPosition.getColumn() + cDirection;
        int r = myPosition.getRow() + rDirection;
        if (c > 0 && c < 9 && r > 0 && r < 9) {
            ChessPosition pos = new ChessPosition(r, c);
            ChessPiece target = board.getPiece(pos);

            if (target == null || target.getTeamColor() != color) {
                moves.add(new ChessMove(myPosition, pos, null));
            }
        }
    }
}
