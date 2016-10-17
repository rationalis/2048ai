package pkg2048;

/**
 * Primarily ported code from:
 * http://stackoverflow.com/a/22498940/1277472
 * https://github.com/nneonneo/2048-ai
 *
 * Provides bitwise ops on 64-bit board representation
 *
 * @author Jimmy
 */
public class Board {

    // top rows are leftmost bits; left columns are leftmost bits
    public static final int[] R_SHIFT_TABLE = new int[1 << 16];
    public static final int[] L_SHIFT_TABLE = new int[1 << 16];
    public static final int[] ROW_SCORE_TABLE = new int[1 << 16];
    public static final byte[] OPEN_COUNT_TABLE = new byte[1 << 16];
    public static final int ROW_MASK = 0xFFFF;
    public static final long COL_MASK = 0x000F000F000F000FL;

    static {
        for (int x = 0; x < 1 << 16; x++) {
            R_SHIFT_TABLE[x] = reverse(shiftLeft(reverse(x)));
            L_SHIFT_TABLE[x] = shiftLeft(x);
            ROW_SCORE_TABLE[x] = rowScore(x);
            OPEN_COUNT_TABLE[x] = countOpen(x);
        }
    }

    public static final long transpose(long x) {
        long a1 = x & 0xF0F00F0FF0F00F0FL;
        long a2 = x & 0x0000F0F00000F0F0L;
        long a3 = x & 0x0F0F00000F0F0000L;
        long a = a1 | (a2 << 12) | (a3 >>> 12);
        long b1 = a & 0xFF00FF0000FF00FFL;
        long b2 = a & 0x00FF00FF00000000L;
        long b3 = a & 0x00000000FF00FF00L;
        return b1 | (b2 >>> 24) | (b3 << 24);
    }

    private static int reverse(int r) {
        int s = 0;
        s |= (r >>> 12) & 0x000F;
        s |= (r >>> 4) & 0x00F0;
        s |= (r << 4) & 0x0F00;
        s |= (r << 12) & 0xF000;
        return s;
    }

    private static long rowToCol(int r) {
        long l = r;
        return (l | (l << 12) | (l << 24) | (l << 36)) & COL_MASK;
    }

    private static byte countOpen(int r) {
        int[] tiles = tilesFromRow(r);
        byte sum = 0;
        for (int tile : tiles) {
            if (tile == 0) {
                sum++;
            }
        }
        return sum;
    }

    private static int shiftLeft(int r) {
        int[] x = tilesFromRow(r);

        for (int i = 0; i < 3; i++) {
            int j;
            for (j = i + 1; j < 4; j++) {
                if (x[j] != 0) {
                    break;
                }
            }
            if (j == 4) {
                break;
            }
            if (x[i] == 0) {
                x[i] = x[j];
                x[j] = 0;
                i--;
            } else if (x[i] == x[j] && x[i] != 0xf) {
                x[i]++;
                x[j] = 0;
            }
        }

        int ans = 0;
        ans |= x[3];
        ans |= x[2] << 4;
        ans |= x[1] << 8;
        ans |= x[0] << 12;
        return ans;
    }
    
    private static long shiftBoard(long b, int[] table) {
        long row4 = ((long) table[(int) b & ROW_MASK]);
        long row3 = ((long) table[(int) (b >>> 16) & ROW_MASK]) << 16;
        long row2 = ((long) table[(int) (b >>> 32) & ROW_MASK]) << 32;
        long row1 = ((long) table[(int) (b >>> 48) & ROW_MASK]) << 48;
        long ans = row1 | row2 | row3 | row4;
        return ans;
    }

    private static long shiftBoardRight(long b) {
        return shiftBoard(b, R_SHIFT_TABLE);
    }

    private static long shiftBoardLeft(long b) {
        return shiftBoard(b, L_SHIFT_TABLE);
    }

    private static long shiftBoardUp(long b) {
        return transpose(shiftBoardLeft(transpose(b)));
    }

    private static long shiftBoardDown(long b) {
        return transpose(shiftBoardRight(transpose(b)));
    }
    
    public static final long shift(long b, int i) {
        long b2 = b;
        switch (i) {
            case 0:
                b2 = shiftBoardLeft(b);
                break;
            case 1:
                b2 = shiftBoardRight(b);
                break;
            case 2:
                b2 = shiftBoardDown(b);
                break;
            case 3:
                b2 = shiftBoardUp(b);
                break;
        }
        return b2;
    }

    public static final long insert(long b, boolean isTwo, int index) {
        long tmp = b;
        long tile = isTwo ? 1 : 2;
        while (true) {
            while ((tmp & 0xf) != 0) {
                tmp >>>= 4;
                tile <<= 4;
            }
            if (index == 0) {
                break;
            }
            index--;
            tmp >>>= 4;
            tile <<= 4;
        }
        return b | tile;
    }

    public static final boolean dead(long b) {
        return shiftBoardRight(b) == b
                && shiftBoardUp(b) == b
                && shiftBoardLeft(b) == b
                && shiftBoardDown(b) == b;
    }

    public static final int rowScore(int r) {
        int[] x = tilesFromRow(r);
        int score = 0;
        for (int i = 0; i < 4; i++) {
            if (x[i] >= 2) {
                score += (x[i] - 1) * (1 << x[i]);
            }
        }
        return score;
    }

    public static final int emptySquares(long b) {
        int[] rows = rowsFromBoard(b);
        return OPEN_COUNT_TABLE[rows[0]]
                + OPEN_COUNT_TABLE[rows[1]]
                + OPEN_COUNT_TABLE[rows[2]]
                + OPEN_COUNT_TABLE[rows[3]];
    }

    public static final int score(long b, int foursSpawned) {
        int[] rows = rowsFromBoard(b);
        return ROW_SCORE_TABLE[rows[0]]
                + ROW_SCORE_TABLE[rows[1]]
                + ROW_SCORE_TABLE[rows[2]]
                + ROW_SCORE_TABLE[rows[3]]
                - foursSpawned * 4;
    }

    public static final int[] tilesFromRow(int r) {
        int[] x = {
            ((r >>> 12) & 0xf),
            ((r >>> 8) & 0xf),
            ((r >>> 4) & 0xf),
            (r & 0xf)
        };
        return x;
    }

    public static final int[] rowsFromBoard(long b) {
        return new int[]{
            (int) (b >>> 48) & ROW_MASK,
            (int) (b >>> 32) & ROW_MASK,
            (int) (b >>> 16) & ROW_MASK,
            (int) b & ROW_MASK};
    }

    public static final int[][] tilesFromBoard(long b) {
        int[] rows = rowsFromBoard(b);
        int[][] tiles = {
            tilesFromRow(rows[0]),
            tilesFromRow(rows[1]),
            tilesFromRow(rows[2]),
            tilesFromRow(rows[3])};
        return tiles;
    }
}
