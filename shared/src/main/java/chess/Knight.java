package chess;

import chess.ChessPiece.PieceType;
import java.util.Collection;
import java.util.HashSet;

public class Knight {

    protected ChessGame.TeamColor color;
    protected ChessPiece.PieceType pieceType;
    public Knight(ChessGame.TeamColor color) {
        this.color = color;
        this.pieceType = PieceType.KNIGHT;
    }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new HashSet();
        this.shift(board, myPosition, moves, -1, 2);
        this.shift(board, myPosition, moves, -2, 1);
        this.shift(board, myPosition, moves, -2, -1);
        this.shift(board, myPosition, moves, -1, -2);
        this.shift(board, myPosition, moves, 1, -2);
        this.shift(board, myPosition, moves, 2, -1);
        this.shift(board, myPosition, moves, 2, 1);
        this.shift(board, myPosition, moves, 1, 2);
        return moves;
    }

    private void shift(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves, int cDirection, int rDirection) {
        int c = myPosition.getColumn() + cDirection;
        int r = myPosition.getRow() + rDirection;
        if (c > 0 && c < 9 && r > 0 && r < 9) {
            ChessPosition pos = new ChessPosition(r, c);
            if (board.getPiece(pos) == null) {
                moves.add(new ChessMove((ChessPosition) myPosition, pos, (ChessPiece.PieceType)null));
            } else if (board.getPiece(pos).getTeamColor() != this.color) {
                moves.add(new ChessMove((ChessPosition) myPosition, pos, (ChessPiece.PieceType)null));
            }
        }

    }
}

