package chess;

import java.util.Arrays;

/**
 * A squares that can hold and rearrange chess pieces.
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessBoard {

    private ChessPiece[][] squares = new ChessPiece[8][8];

    public ChessBoard() {

    }

    /**
     * Adds a chess piece to the squares
     *
     * @param position where to add the piece to
     * @param piece    the piece to add
     */
    public void addPiece(ChessPosition position, ChessPiece piece) {
        //throw new RuntimeException("Not implemented");
        squares[position.getRow() - 1][position.getColumn() - 1] = piece;
    }

    /**
     * Gets a chess piece on the squares
     *
     * @param position The position to get the piece from
     * @return Either the piece at the position, or null if no piece is at that
     * position
     */
    public ChessPiece getPiece(ChessPosition position) {
        //throw new RuntimeException("Not implemented");
        return squares[position.getRow() - 1][position.getColumn() - 1];
    }

    /**
     * Sets the board to the default starting board
     * (How the game of chess normally starts)
     */
    public void resetBoard() {
        //throw new RuntimeException("Not implemented");
        clearBoard();
        setSquares();
    }

    private void clearBoard() {
        for (int i = 0; i < 8; ++i) {
            for (int j = 0; j < 8; ++j) {
                this.squares[i][j] = null;
            }
        }

    }

    private void setSquares() {
        int c = 0;
        int w = 0;
        int b = 7;
        this.squares[c][w] = new Rook(ChessGame.TeamColor.WHITE);
        this.squares[c][b] = new Rook(ChessGame.TeamColor.BLACK);
        ++c;

        this.squares[c][w] = new King(ChessGame.TeamColor.WHITE);
        this.squares[c][b] = new King(ChessGame.TeamColor.BLACK);
        ++c;

        this.squares[c][w] = new Rook(ChessGame.TeamColor.WHITE);
        this.squares[c][b] = new Rook(ChessGame.TeamColor.BLACK);
        ++w;
        --b;

    }
}