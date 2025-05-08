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
        this.shift(board, myPosition, moves, -1, 0);
        this.shift(board, myPosition, moves, -1, 1);
        this.shift(board, myPosition, moves, 0, 1);
        this.shift(board, myPosition, moves, 1, 1);
        this.shift(board, myPosition, moves, 1, 0);
        this.shift(board, myPosition, moves, 1, -1);
        this.shift(board, myPosition, moves, 0, -1);
        this.shift(board, myPosition, moves, -1, -1);
        return moves;
    }

    private void shift(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves, int c_direction, int r_direction) {
        int c = myPosition.getColumn() + c_direction;
        int r = myPosition.getRow() + r_direction;
        if (c > 0 && c < 9 && r > 0 && r < 9) {
            ChessPosition pos = new ChessPosition(c, r);
            if (board.getPiece(pos) == null) {
                moves.add(new ChessMove((ChessPosition) myPosition, pos, (ChessPiece.PieceType)null));
            } else if (board.getPiece(pos).getTeamColor() != this.color) {
                moves.add(new ChessMove((ChessPosition) myPosition, pos, (ChessPiece.PieceType)null));
            }
        }

    }
}

