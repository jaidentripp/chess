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
        ChessPosition oneStep = new ChessPosition(c, r + direction);
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
                ChessPosition twoStep = new ChessPosition(c, r + 2 * direction);
                if (board.getPiece(twoStep) == null) {
                    moves.add(new ChessMove(myPosition, twoStep, null));
                }
            }
        }

        //capture diagonally left
        ChessPosition diagLeft = new ChessPosition(c - 1, r + direction);
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
        ChessPosition diagRight = new ChessPosition(c + 1, r + direction);
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

//    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
//        this.color = board.getPiece(myPosition).getTeamColor();
//        int r = myPosition.getRow();
//        int c = myPosition.getColumn();
//        Collection<ChessMove> moves = new HashSet();
//        Collection<ChessPiece.PieceType> promotions = new ArrayList();
//        Iterator var7;
//        ChessPiece.PieceType p;
//        if (this.color == TeamColor.WHITE) {
//            if (r == 2 && board.getPiece(new ChessPosition(c, 4)) == null && board.getPiece(new ChessPosition(c, 3)) == null) {
//                this.shift(board, myPosition, moves, 0, 2, (ChessPiece.PieceType)null);
//            }
//
//            if (r == 7) {
//                promotions.add(PieceType.ROOK);
//                promotions.add(PieceType.KNIGHT);
//                promotions.add(PieceType.QUEEN);
//                promotions.add(PieceType.BISHOP);
//            } else {
//                promotions.add((Object)null);
//            }
//
//            if (board.getPiece(new MyChessPosition(c, r + 1)) == null) {
//                var7 = promotions.iterator();
//
//                while(var7.hasNext()) {
//                    p = (ChessPiece.PieceType)var7.next();
//                    this.shift(board, myPosition, moves, 0, 1, p);
//                }
//            }
//
//            if (c > 1 && board.getPiece(new MyChessPosition(c - 1, r + 1)) != null && board.getPiece(new MyChessPosition(c - 1, r + 1)).getTeamColor() != this.color) {
//                var7 = promotions.iterator();
//
//                while(var7.hasNext()) {
//                    p = (ChessPiece.PieceType)var7.next();
//                    this.shift(board, myPosition, moves, -1, 1, p);
//                }
//            }
//
//            if (c < 8 && board.getPiece(new MyChessPosition(c + 1, r + 1)) != null && board.getPiece(new MyChessPosition(c + 1, r + 1)).getTeamColor() != this.color) {
//                var7 = promotions.iterator();
//
//                while(var7.hasNext()) {
//                    p = (ChessPiece.PieceType)var7.next();
//                    this.shift(board, myPosition, moves, 1, 1, p);
//                }
//            }
//        } else {
//            if (r == 7 && board.getPiece(new MyChessPosition(c, 5)) == null && board.getPiece(new MyChessPosition(c, 6)) == null) {
//                this.shift(board, myPosition, moves, 0, -2, (ChessPiece.PieceType)null);
//            }
//
//            if (r == 2) {
//                promotions.add(PieceType.ROOK);
//                promotions.add(PieceType.KNIGHT);
//                promotions.add(PieceType.QUEEN);
//                promotions.add(PieceType.BISHOP);
//            } else {
//                promotions.add((Object)null);
//            }
//
//            if (board.getPiece(new MyChessPosition(c, r - 1)) == null) {
//                var7 = promotions.iterator();
//
//                while(var7.hasNext()) {
//                    p = (ChessPiece.PieceType)var7.next();
//                    this.shift(board, myPosition, moves, 0, -1, p);
//                }
//            }
//
//            if (c > 1 && board.getPiece(new MyChessPosition(c - 1, r - 1)) != null && board.getPiece(new MyChessPosition(c - 1, r - 1)).getTeamColor() != this.color) {
//                var7 = promotions.iterator();
//
//                while(var7.hasNext()) {
//                    p = (ChessPiece.PieceType)var7.next();
//                    this.shift(board, myPosition, moves, -1, -1, p);
//                }
//            }
//
//            if (c < 8 && board.getPiece(new MyChessPosition(c + 1, r - 1)) != null && board.getPiece(new MyChessPosition(c + 1, r - 1)).getTeamColor() != this.color) {
//                var7 = promotions.iterator();
//
//                while(var7.hasNext()) {
//                    p = (ChessPiece.PieceType)var7.next();
//                    this.shift(board, myPosition, moves, 1, -1, p);
//                }
//            }
//        }
//
//        return moves;
//    }

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

//    private void shift(ChessBoard board, ChessPosition myPosition, Collection<ChessMove> moves, int c_direction, int r_direction, ChessPiece.PieceType promo) {
//        int c = myPosition.getColumn();
//        int r = myPosition.getRow();
//        c += c_direction;
//        r += r_direction;
//        if (c > 0 && c < 9 && r > 0 && r < 9) {
//            MyChessPosition pos = new MyChessPosition(c, r);
//            if (board.getPiece(pos) == null) {
//                moves.add(new MyChessMove((MyChessPosition)myPosition, pos, promo));
//            } else if (board.getPiece(pos).getTeamColor() != this.color) {
//                moves.add(new MyChessMove((MyChessPosition)myPosition, pos, promo));
//            }
//        }
//    }
}
