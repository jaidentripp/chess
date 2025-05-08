package chess;

import chess.ChessGame.TeamColor;
import chess.ChessPiece.PieceType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class Pawn {

    protected ChessGame.TeamColor color;
    protected ChessPiece.PieceType pieceType;

    public Pawn(ChessGame.TeamColor color) {
        this.color = color;
        this.pieceType = PieceType.PAWN;
    }

    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        Collection<ChessMove> moves = new HashSet<>();

        int c = myPosition.getColumn();
        int r = myPosition.getRow();

        int direction = (this.color == TeamColor.WHITE) ? 1 : -1;
        int promotionRow = (this.color == TeamColor.WHITE) ? 8 : 1;

        //one square forward
        ChessPosition oneStep = new ChessPosition(r + direction, c );
        if (isValidPosition(oneStep) && board.getPiece(oneStep) == null) {
            if (oneStep.getRow() == promotionRow) {
                addPromotionMoves(moves, myPosition, oneStep);
            } else {
                moves.add(new ChessMove(myPosition, oneStep, null));
            }

            //two squares forward from starting position
            boolean isAtStartRow = (this.color == TeamColor.WHITE && r == 2) || (this.color ==
                    TeamColor.BLACK && r == 7);
            if (isAtStartRow) {
                ChessPosition twoStep = new ChessPosition(r + 2 * direction, c);
                if (board.getPiece(twoStep) == null) {
                    moves.add(new ChessMove(myPosition, twoStep, null));
                }
            }
        }

        //capture diagonally left
        ChessPosition diagLeft = new ChessPosition(r + direction, c - 1);
        if (isValidPosition(diagLeft)) {
            ChessPiece pieceAtDiagLeft = board.getPiece(diagLeft);
            if (pieceAtDiagLeft != null && pieceAtDiagLeft.getTeamColor() != this.color) {
                if (diagLeft.getRow() == promotionRow) {
                    addPromotionMoves(moves, myPosition, diagLeft);
                } else {
                    moves.add(new ChessMove(myPosition, diagLeft, null));
                }
            }
        }

        //capture diagonally right
        ChessPosition diagRight = new ChessPosition(r + direction, c + 1);
        if (isValidPosition(diagRight)) {
            ChessPiece pieceAtDiagRight = board.getPiece(diagRight);
            if (pieceAtDiagRight != null && pieceAtDiagRight.getTeamColor() != this.color) {
                if (diagRight.getRow() == promotionRow) {
                    addPromotionMoves(moves, myPosition, diagRight);
                } else {
                    moves.add(new ChessMove(myPosition, diagRight, null));
                }
            }
        }

        return moves;
    }

    private boolean isValidPosition(ChessPosition pos) {
        int c = pos.getColumn();
        int r = pos.getRow();
        return c > 0 && c < 9 && r > 0 && r < 9;
    }

    private void addPromotionMoves(Collection<ChessMove> moves, ChessPosition from, ChessPosition to) {
        moves.add(new ChessMove(from, to, PieceType.QUEEN));
        moves.add(new ChessMove(from, to, PieceType.ROOK));
        moves.add(new ChessMove(from, to, PieceType.BISHOP));
        moves.add(new ChessMove(from, to, PieceType.KNIGHT));
    }
}
