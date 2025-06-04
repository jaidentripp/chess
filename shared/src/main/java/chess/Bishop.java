package chess;

import chess.ChessPiece.PieceType;
import java.util.Collection;
import java.util.HashSet;

public class Bishop {

    protected ChessGame.TeamColor color;
    protected ChessPiece.PieceType pieceType;
    public Bishop(ChessGame.TeamColor color) {
        this.color = color;
        this.pieceType = PieceType.BISHOP;
    }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new HashSet();
        this.shift(board, myPosition, moves, 1, 1);
        this.shift(board, myPosition, moves, -1, 1);
        this.shift(board, myPosition, moves, 1, -1);
        this.shift(board, myPosition, moves, -1, -1);
        return moves;
    }

    private void shift(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves, int cDirection, int rDirection) {
        int c = myPosition.getColumn() + cDirection;
        int r = myPosition.getRow() + rDirection;

        // Continue moving in the direction until we hit the edge or a piece
        while (c >= 1 && c <= 8 && r >= 1 && r <= 8) {
            ChessPosition pos = new ChessPosition(r, c);
            ChessPiece piece = board.getPiece(pos);

            if (piece == null) {
                // Empty square, add move and continue
                moves.add(new ChessMove(myPosition, pos, null));
            } else {
                // There is a piece here
                if (piece.getTeamColor() != this.color) {
                    // Enemy piece, can capture, add move
                    moves.add(new ChessMove(myPosition, pos, null));
                }
                // Stop moving further in this direction after hitting any piece
                break;
            }

            // Move further in the same direction
            c += cDirection;
            r += rDirection;
        }
    }

}

