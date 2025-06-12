package chess;

import java.util.Objects;

/**
 * Represents a single square position on a chess board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPosition {

    private final int row;
    private final int col;


    public ChessPosition(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /**
     * @return which row this position is in
     * 1 codes for the bottom row
     */
    public int getRow() {
        //throw new RuntimeException("Not implemented");
        return row;
    }

    /**
     * @return which column this position is in
     * 1 codes for the left row
     */
    public int getColumn() {
        //throw new RuntimeException("Not implemented");
        return col;
    }

    public static ChessPosition fromAlgebraic(String pos) {
        if (pos == null) throw new IllegalArgumentException("Null position string");
        pos = pos.trim();
        // Format: e2
        if (pos.length() == 2 && Character.isLetter(pos.charAt(0)) && Character.isDigit(pos.charAt(1))) {
            char file = pos.charAt(0);
            char rank = pos.charAt(1);
            int column = file - 'a' + 1; // 'a' -> 1
            int row = Character.getNumericValue(rank); // '1' -> 1
            return new ChessPosition(row, column);
        }
        // Format: 25 (row=2, col=5)
        if (pos.length() == 2 && Character.isDigit(pos.charAt(0)) && Character.isDigit(pos.charAt(1))) {
            int row = Character.getNumericValue(pos.charAt(0));
            int column = Character.getNumericValue(pos.charAt(1));
            return new ChessPosition(row, column);
        }
        throw new IllegalArgumentException("Invalid position string: " + pos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPosition that = (ChessPosition) o;
        return row == that.row && col == that.col;
    }

    @Override
    public String toString() {
        return "ChessPosition{" +
                "row=" + row +
                ", col=" + col +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}
