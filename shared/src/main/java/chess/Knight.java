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
        KingKnightShift.shift(board, myPosition, moves, -1, 2, this.color);
        KingKnightShift.shift(board, myPosition, moves, -2, 1, this.color);
        KingKnightShift.shift(board, myPosition, moves, -2, -1, this.color);
        KingKnightShift.shift(board, myPosition, moves, -1, -2, this.color);
        KingKnightShift.shift(board, myPosition, moves, 1, -2, this.color);
        KingKnightShift.shift(board, myPosition, moves, 2, -1, this.color);
        KingKnightShift.shift(board, myPosition, moves, 2, 1, this.color);
        KingKnightShift.shift(board, myPosition, moves, 1, 2, this.color);
        return moves;
    }
}

