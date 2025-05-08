package chess;

import chess.ChessPiece.PieceType;
import java.util.Collection;
import java.util.HashSet;

public class Queen {
    protected ChessGame.TeamColor color;
    protected ChessPiece.PieceType pieceType;
    public Queen(ChessGame.TeamColor color) {
        this.color = color;
        this.pieceType = PieceType.QUEEN;
    }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new HashSet();
        this.shift(board, myPosition, moves, 1, 1);
        this.shift(board, myPosition, moves, 1, 0);
        this.shift(board, myPosition, moves, 1, -1);
        this.shift(board, myPosition, moves, 0, -1);
        this.shift(board, myPosition, moves, -1, -1);
        this.shift(board, myPosition, moves, -1, 0);
        this.shift(board, myPosition, moves, -1, 1);
        this.shift(board, myPosition, moves, 0, 1);
        return moves;
    }

    private void shift(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves, int c_direction, int r_direction) {
        int c = myPosition.getColumn();
        int r = myPosition.getRow();

        while(true) {
            c += c_direction;
            r += r_direction;
            if (c <= 0 || c >= 9 || r <= 0 || r >= 9) {
                return;
            }

            ChessPosition pos = new ChessPosition(r, c);
            if (board.getPiece(pos) != null) {
                if (board.getPiece(pos).getTeamColor() != this.color) {
                    moves.add(new ChessMove((ChessPosition) myPosition, pos, (ChessPiece.PieceType)null));
                    return;
                }

                return;
            }

            moves.add(new ChessMove((ChessPosition) myPosition, pos, (ChessPiece.PieceType)null));
        }
    }
}

