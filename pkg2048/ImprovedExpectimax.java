package pkg2048;

import static java.lang.Integer.bitCount;
import static java.lang.Math.min;
import static java.lang.Math.max;
import static java.lang.Math.pow;

import static pkg2048.Board.*;

/**
 *
 * @author Jimmy
 */
public class ImprovedExpectimax extends Expectimax {
    
    static final float SCORE_LOST_PENALTY = 200000.0f;
    static final float SCORE_MONOTONICITY_POWER = 4.0f;
    static final float SCORE_MONOTONICITY_WEIGHT = 47.0f;
    static final float SCORE_SUM_POWER = 3.5f;
    static final float SCORE_SUM_WEIGHT = 11.0f;
    static final float SCORE_MERGES_WEIGHT = 700.0f;
    static final float SCORE_EMPTY_WEIGHT = 270.0f;
    
    @Override
    public int nextMove(long b) {
        int distinctTiles = countDistinctTiles(b);
        probThresh = distinctTiles < 7 ? 0.001f : 0.0001f;
        cacheLimit = 6;
        depthLimit = max(3, distinctTiles - 2);
        return findBestMove(b);
    }
    
    public int countDistinctTiles(long b) {
        int bitset = 0;
        while (b != 0) {
            bitset |= 1 << (b & 0xf);
            b >>>= 4;
        }
        bitset >>>= 1;
        return bitCount(bitset);
    }
    
    @Override
    public float heuristicRow(int r) {
        int[] tile = tilesFromRow(r);

        float sum = 0;
        int empty = 0;
        int merges = 0;
        int prev = 0;
        int counter = 0;

        for (int i = 0; i < 4; i++) {
            int rank = tile[i];
            sum += pow(rank, SCORE_SUM_POWER);
            if (rank == 0) {
                empty++;
            } else {
                if (prev == rank) {
                    counter++;
                } else if (counter > 0) {
                    merges += 1 + counter;
                    counter = 0;
                }
                prev = rank;
            }
        }

        if (counter > 0) {
            merges += 1 + counter;
        }

        float monotonicityLeft = 0;
        float monotonicityRight = 0;
        for (int i = 1; i < 4; i++) {
            if (tile[i-1] > tile[i]) {
                monotonicityLeft +=
                        pow(tile[i-1], SCORE_MONOTONICITY_POWER) -
                        pow(tile[i], SCORE_MONOTONICITY_POWER);
            } else {
                monotonicityRight +=
                        pow(tile[i], SCORE_MONOTONICITY_POWER) -
                        pow(tile[i-1], SCORE_MONOTONICITY_POWER);
            }
        }
 
        return
            SCORE_LOST_PENALTY +
            SCORE_EMPTY_WEIGHT * empty +
            SCORE_MERGES_WEIGHT * merges -
            SCORE_MONOTONICITY_WEIGHT *
                min(monotonicityLeft, monotonicityRight) -
            SCORE_SUM_WEIGHT * sum;
    }
    
    @Override
    public float heuristic(long b) {
        long b2 = transpose(b);
        int[] rows = {
            (int) (b >>> 48) & ROW_MASK,
            (int) (b >>> 32) & ROW_MASK,
            (int) (b >>> 16) & ROW_MASK,
            (int) b & ROW_MASK,
            (int) (b2 >>> 48) & ROW_MASK,
            (int) (b2 >>> 32) & ROW_MASK,
            (int) (b2 >>> 16) & ROW_MASK,
            (int) b2 & ROW_MASK};
        float h = 0;
        for (int row : rows) {
            h += heuristicTable[row];
        }
        return h;
    }
}
