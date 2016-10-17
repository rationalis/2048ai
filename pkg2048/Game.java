package pkg2048;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static pkg2048.Board.*;

public class Game extends JPanel {

    private static final Color BG_COLOR = new Color(0xbbada0);
    private static final String FONT_NAME = "Arial";
    private static final int TILE_SIZE = 64;
    private static final int TILES_MARGIN = 16;

    private static final int[] TILE_BG_COLORS =
        new int[]{
            0xcdc1b4, 0xeee4da, 0xede0c8, 0xf2b179, 0xf59563, 0xf67c5f,
            0xf65e3b, 0xedcf72, 0xedcc61, 0xedc850, 0xedc53f, 0xedc22e,
            0x009933, 0x0066FF, 0xCC0000, 0x000000};

    private boolean myLose = false;
    private long board = 0;
    private int foursSpawned = 0;

    private boolean ai = false;

    public Game() {
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    board = 0xFFFFFFFFFFFFFFFFL;
                    repaint();
                    return;
                }
                if (!ai) {
                    AI x = null;
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_1: x = AI.randomPlayer(); break;
                        case KeyEvent.VK_2: x = AI.naivePlayer(); break;
                        case KeyEvent.VK_3: x = new Expectimax(); break;
                        case KeyEvent.VK_4: x = new ImprovedExpectimax(); break;
                    }
                    if (x != null) {
                        ai = true;
                        AI x2 = x;
                        new Thread(() -> aiPlay(x2)).start();
                        return;
                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    resetGame();
                    repaint();
                    return;
                } else if (ai) {
                    return;
                }

                if (!myLose) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT:
                            move(0);
                            break;
                        case KeyEvent.VK_RIGHT:
                            move(1);
                            break;
                        case KeyEvent.VK_DOWN:
                            move(2);
                            break;
                        case KeyEvent.VK_UP:
                            move(3);
                            break;
                    }
                }
                repaint();
            }
        });
        resetGame();
    }

    public void move(int i) {
        if (dead(board)) {
            myLose = true;
            return;
        }
        long board2 = shift(board, i);
        if (board2 != board) {
            board = board2;
            insertRandom();
        }
    }

    public void resetGame() {
        myLose = false;
        board = 0;
        foursSpawned = 0;
        insertRandom();
        insertRandom();
    }

    public void insertRandom() {
        boolean isTwo = Math.random() < 0.9;
        if (!isTwo) {
            foursSpawned++;
        }
        int empty = emptySquares(board);
        board = insert(board, isTwo, (int) (Math.random() * (empty - 1)));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, this.getSize().width, this.getSize().height);
        int[][] tiles = tilesFromBoard(board);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                drawTile(g, tiles[y][x], x, y);
            }
        }
    }

    private void drawTile(Graphics g2, int tile, int x, int y) {
        Graphics2D g = ((Graphics2D) g2);
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(
                RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_NORMALIZE);
        int value = tile == 0 ? 0 : 1 << tile;
        int xOffset = offsetCoors(x);
        int yOffset = offsetCoors(y);
        g.setColor(new Color(TILE_BG_COLORS[tile]));
        g.fillRoundRect(xOffset, yOffset, TILE_SIZE, TILE_SIZE, 14, 14);
        Color foreground =
                value < 16 ? new Color(0x776e65) : new Color(0xf9f6f2);
        g.setColor(foreground);
        int size = getSize(value);
        Font font = new Font(FONT_NAME, Font.BOLD, size);
        g.setFont(font);

        String s = String.valueOf(value);
        FontMetrics fm = getFontMetrics(font);

        int w = fm.stringWidth(s);
        int h = -(int) fm.getLineMetrics(s, g).getBaselineOffsets()[2];

        if (value != 0) {
            g.drawString(s,
                    xOffset + (TILE_SIZE - w) / 2,
                    yOffset + TILE_SIZE - (TILE_SIZE - h) / 2 - 2);
        }

        if (myLose) {
            g.setFont(new Font(FONT_NAME, Font.PLAIN, 18));
            g.setColor(new Color(128, 128, 128, 128));
            g.drawString("You lose! Press ESC to play again", 40, getHeight() - 30);
        }
        g.setFont(new Font(FONT_NAME, Font.PLAIN, 22));
        g.setColor(new Color(128, 128, 128, 128));
        g.drawString("Score: " + score(board, foursSpawned), 180, 365);

    }

    private static int offsetCoors(int arg) {
        return arg * (TILES_MARGIN + TILE_SIZE) + TILES_MARGIN;
    }
    
    public static int getSize(int value) {
        if (value < 100) {
            return 36;
        } else if (value < 1000) {
            return 32;
        } else if (value < 10000) {
            return 24;
        } else {
            return 20;
        }
    }

    public void aiPlay(AI player) {
        int moveCounter = 0;
        long init = System.currentTimeMillis();
        while (!myLose) {
            move(player.nextMove(board));
            repaint();
            moveCounter++;
        }
        long timeElapsed = System.currentTimeMillis() - init;
        float speed = moveCounter / (timeElapsed / 1000.0f);
        String spdstr = Float.toString(speed);
        if (spdstr.length() > 7) {
            spdstr = spdstr.substring(0,7);
        }
        System.out.println(
                "Score: " + score(board, foursSpawned) + "; " +
                moveCounter + " moves at " +
                spdstr + " moves/second");
        ai = false;
        pauseAndRepaint();
    }

    public void pauseAndRepaint() {
        try {
            Thread.sleep(300);
            repaint();
        } catch (InterruptedException e) {
        }
    }

    public static void main(String[] args) {
        JFrame game = new JFrame();
        game.setTitle("2048 Game");
        game.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        game.setSize(340, 420);
        game.setResizable(false);

        game.add(new Game());

        game.setLocationRelativeTo(null);
        game.setAlwaysOnTop(true);
        game.setVisible(true);
    }
}
