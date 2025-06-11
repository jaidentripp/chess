package chess;

import chess.ChessPiece.PieceType;
import java.util.Collection;
import java.util.HashSet;

public class King {

    protected ChessGame.TeamColor color;
    protected ChessPiece.PieceType pieceType;
    public King(ChessGame.TeamColor color) {
        this.color = color;
        this.pieceType = PieceType.KING;
    }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new HashSet();
        KingKnightShift.shift(board, myPosition, moves, -1, 0, this.color);
        KingKnightShift.shift(board, myPosition, moves, -1, 1, this.color);
        KingKnightShift.shift(board, myPosition, moves, 0, 1, this.color);
        KingKnightShift.shift(board, myPosition, moves, 1, 1, this.color);
        KingKnightShift.shift(board, myPosition, moves, 1, 0, this.color);
        KingKnightShift.shift(board, myPosition, moves, 1, -1, this.color);
        KingKnightShift.shift(board, myPosition, moves, 0, -1, this.color);
        KingKnightShift.shift(board, myPosition, moves, -1, -1, this.color);
        return moves;
    }
}

