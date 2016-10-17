package pkg2048;

import java.util.HashMap;

import java.util.concurrent.ForkJoinTask;
import static java.util.concurrent.ForkJoinTask.adapt;
import static java.util.concurrent.ForkJoinTask.invokeAll;

import static pkg2048.Board.*;

/**
 *
 * @author Jimmy
 */
public class Expectimax implements AI {
    
    final float[] heuristicTable;
    float probThresh;
    int depthLimit;
    int cacheLimit;

    public Expectimax() {
        heuristicTable = new float[1 << 16];
            for (int x = 0; x < 1 << 16; x++) {
                heuristicTable[x] = heuristicRow(x);
        }
    }
    
    public int nextMove(long b) {
        probThresh = 0.0001f;
        int score = score(b,0);
        cacheLimit = score < 1 << 12 ? -1 : 4;
        depthLimit = score < 1 << 12 ? 2 : 6;
        return findBestMove(b);
    }

    public float heuristicRow(int r) {
        int[] tile = tilesFromRow(r);
        float heur = 0;
        int maxi = 0;
        for (int i = 0; i < 4; i++) {
            maxi = tile[i] > tile[maxi] ? i : maxi;
            heur += tile[i] == 0 ? 10000 : 0;
            heur += i > 0 && Math.abs(tile[i] - tile[i-1]) == 1 ? 1000 : 0;
        }
        if (maxi == 0 || maxi == 3) {
            heur += 20000;
        }
        if (((tile[0] < tile[1])
                && (tile[1] < tile[2])
                && (tile[2] < tile[3]))
                || ((tile[0] > tile[1])
                && (tile[1] > tile[2])
                && (tile[2] > tile[3]))) {
            heur += 10000;
        }
        return heur;
    }

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
        float h = 100000;
        for (int row : rows) {
            h += heuristicTable[row];
        }
        return h;
    }

    public float scoreRandNode(
            long b, float cprob, int curDepth, HashMap<Long, Float> map) {
        int open = emptySquares(b);
        cprob /= open;
        float ans = 0;
        long tmp = b;
        long tile = 1;
        while (tile != 0) {
            if ((tmp & 0xf) == 0) {
                ans += scoreMoveNode(b | tile,
                        cprob * 0.9f, curDepth, map) * 0.9f;
                ans += scoreMoveNode(b | (tile << 1),
                        cprob * 0.1f, curDepth, map) * 0.1f;
            }
            tmp >>>= 4;
            tile <<= 4;
        }
        return ans / open;
    }
    
    public float scoreMoveNode(
            long b, float cprob, int curDepth, HashMap<Long, Float> map) {
        if (cprob < probThresh || curDepth >= depthLimit) {
            return heuristic(b);
        }

        Float temp;
        if (curDepth < cacheLimit && ((temp = map.get(b)) != null)) {
            return temp;
        }

        float best = 0;
        for (int i = 0; i < 4; i++) {
            long b2 = shift(b, i);
            if (b != b2) {
                float score = scoreRandNode(b2, cprob, curDepth + 1, map);
                best = score > best ? score : best;
            }
        }

        if (curDepth < cacheLimit) {
            map.put(b, best);
        }

        return best;
    }
    
    public int findBestMove(long b) {
        float best = 0;
        int bestMove = (int) (Math.random() * 4);

        ForkJoinTask[] task = new ForkJoinTask[4];

        for (int move = 0; move < 4; move++) {
            long b2 = shift(b, move);
            task[move] = b == b2
                    ? adapt(() -> 0.0f)
                    : adapt(() ->
                            scoreRandNode(
                                    b2, 1.0f, 0, new HashMap<>(200000)));
        }

        invokeAll(task);
        try {
            for (int move = 0; move < 4; move++) {
                float score = (Float) (task[move].get());
                if (score > best) {
                    best = score;
                    bestMove = move;
                }
            }
        } catch (Exception e) {
        }
        
        return bestMove;
    }
}
