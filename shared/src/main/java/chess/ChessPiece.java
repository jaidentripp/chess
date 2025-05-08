package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;


/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    protected ChessGame.TeamColor color;
    protected ChessPiece.PieceType pieceType;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        color = pieceColor;
        pieceType = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        //throw new RuntimeException("Not implemented");
        return color;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        //throw new RuntimeException("Not implemented");
        return pieceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece that = (ChessPiece) o;
        return color == that.color && pieceType == that.pieceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, pieceType);
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        //throw new RuntimeException("Not implemented");
        if (pieceType == PieceType.KING) {
            King king = new King(color);
            Collection<ChessMove> kingMoves = king.pieceMoves(board, myPosition);
            return kingMoves;
        }
        if (pieceType == PieceType.ROOK) {
            Rook rook = new Rook(color);
            Collection<ChessMove> rookMoves = rook.pieceMoves(board, myPosition);
            return rookMoves;
        }
        if (pieceType == PieceType.QUEEN) {
            Queen queen = new Queen(color);
            Collection<ChessMove> queenMoves = queen.pieceMoves(board, myPosition);
            return queenMoves;
        }
        if (pieceType == PieceType.KNIGHT) {
            Knight knight = new Knight(color);
            Collection<ChessMove> knightMoves = knight.pieceMoves(board, myPosition);
            return knightMoves;
        }
        return new ArrayList<>();
    }
}
