package ui;

import chess.ChessBoard;
import chess.ChessPiece;
import chess.ChessPosition;

public class ChessBoardPrinter {
    public static void printBoard(ChessBoard board, boolean whitePerspective) {
        String[] cols = whitePerspective
                ? new String[]{"a", "b", "c", "d", "e", "f", "g", "h"}
                : new String[]{"h", "g", "f", "e", "d", "c", "b", "a"};

        int[] rows = whitePerspective
                ? new int[]{8, 7, 6, 5, 4, 3, 2, 1}
                : new int[]{1, 2, 3, 4, 5, 6, 7, 8};

        //print out column headers
        System.out.print("   ");
        for (String col : cols) {
            System.out.print(" " + col + " ");
        }
        System.out.println();

        for (int row : rows) {
            System.out.print(" " + row + " ");
            for (int colIndex = 0; colIndex < 8; colIndex++) {
                boolean isLight = ((row + colIndex) % 2 == 0);
                String bg = isLight ? EscapeSequences.SET_BG_COLOR_WHITE : EscapeSequences.SET_BG_COLOR_DARK_GREY;
                //ChessPiece piece = board.getPiece(new ChessPosition(getBoardRow(row, whitePerspective), getBoardCol(colIndex, whitePerspective)));
                int boardRow = row;
                int boardCol = whitePerspective ? colIndex + 1 : 8 - colIndex;
                ChessPiece piece = board.getPiece(new ChessPosition(boardRow, boardCol));
                String symbol = pieceToSymbol(piece);
                System.out.print(bg + symbol + EscapeSequences.RESET_BG_COLOR);
            }
            System.out.println(" " + row);
        }

        //print column headers again
        System.out.print("   ");
        for (String col : cols) {
            System.out.print(" " + col + " ");
        }
        System.out.println();
    }

    private static int getBoardRow(int displayedRow, boolean whitePerspective) {
        return whitePerspective ? 8 - displayedRow : displayedRow - 1;
    }

    private static int getBoardCol(int colIndex, boolean whitePerspective) {
        return whitePerspective ? colIndex : 7 - colIndex;
    }

    private static String pieceToSymbol(ChessPiece piece) {
        if (piece == null) {
            return EscapeSequences.EMPTY;
        }
        switch (piece.getTeamColor()) {
            case WHITE -> {
                String color = EscapeSequences.SET_TEXT_COLOR_BLUE;
                return color + switch (piece.getPieceType()) {
                    case KING -> EscapeSequences.WHITE_KING;
                    case QUEEN -> EscapeSequences.WHITE_QUEEN;
                    case BISHOP -> EscapeSequences.WHITE_BISHOP;
                    case KNIGHT -> EscapeSequences.WHITE_KNIGHT;
                    case ROOK -> EscapeSequences.WHITE_ROOK;
                    case PAWN -> EscapeSequences.WHITE_PAWN;
                } + EscapeSequences.RESET_TEXT_COLOR;
            }
            case BLACK -> {
                String color = EscapeSequences.SET_TEXT_COLOR_MAGENTA;
                return color + switch (piece.getPieceType()) {
                    case KING -> EscapeSequences.BLACK_KING;
                    case QUEEN -> EscapeSequences.BLACK_QUEEN;
                    case BISHOP -> EscapeSequences.BLACK_BISHOP;
                    case KNIGHT -> EscapeSequences.BLACK_KNIGHT;
                    case ROOK -> EscapeSequences.BLACK_ROOK;
                    case PAWN -> EscapeSequences.BLACK_PAWN;
                } + EscapeSequences.RESET_TEXT_COLOR;
            }
        }
        return EscapeSequences.EMPTY;
    }
}

