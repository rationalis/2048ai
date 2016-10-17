package pkg2048;

import static pkg2048.Board.*;

/**
 *
 * @author Jimmy
 */
public interface AI {

    public int nextMove(long b);

    public static AI randomPlayer() {
        return b -> (int) (Math.random() * 4);
    }

    public static AI naivePlayer() {
        return b -> {
            long[] score = {
                emptySquares(shift(b,0)),
                emptySquares(shift(b,1)),
                emptySquares(shift(b,2)),
                emptySquares(shift(b,3))
            };

            int maxInd = 0;

            if (score[0] == score[1]
                    && score[1] == score[2]
                    && score[2] == score[3]) {
                return randomPlayer().nextMove(b);
            }

            for (int i = 0; i < 3; i++) {
                if (score[i] > score[maxInd] || shift(b, maxInd) == b) {
                    maxInd = i;
                }
            }
            return maxInd;
        };
    }
}
