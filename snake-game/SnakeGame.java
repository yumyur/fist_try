import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Random;

/**
 * Snake - a small, dependency-free Snake game written in Java (Swing).
 *
 * <p>Run it without compiling anything:
 * <pre>    java SnakeGame.java</pre>
 * or compile and run the classic way:
 * <pre>
 *    javac -d build SnakeGame.java
 *    java -cp build SnakeGame
 * </pre>
 *
 * <h2>Controls</h2>
 * Arrow keys / WASD - steer, SPACE - start &amp; pause, P - pause, R - restart.
 */
public class SnakeGame extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------

    /** Number of grid columns. */
    private static final int COLS = 30;
    /** Number of grid rows. */
    private static final int ROWS = 24;
    /** Pixel size of a single grid cell. */
    private static final int CELL = 24;
    /** Padding around the play field. */
    private static final int PAD = 14;
    /** Height of the score bar above the play field. */
    private static final int HUD = 46;

    /** Milliseconds per step at the start of a game. */
    private static final int BASE_DELAY_MS = 130;
    /** Fastest allowed step time. */
    private static final int MIN_DELAY_MS = 55;
    /** Milliseconds shaved off the step time per point scored. */
    private static final int SPEEDUP_PER_POINT_MS = 3;

    private static final int INITIAL_LENGTH = 4;

    /** Where the session-independent high score is remembered. */
    private static final Path HIGH_SCORE_FILE =
            Paths.get(System.getProperty("user.home", "."), ".snake-highscore");

    private static final Color BG_TOP = new Color(0x12, 0x18, 0x22);
    private static final Color BG_BOTTOM = new Color(0x07, 0x0A, 0x0F);
    private static final Color FIELD_A = new Color(0x18, 0x20, 0x2C);
    private static final Color FIELD_B = new Color(0x14, 0x1B, 0x26);
    private static final Color FIELD_BORDER = new Color(0x2B, 0x3A, 0x4E);
    private static final Color TEXT = new Color(0xE8, 0xEF, 0xF8);
    private static final Color TEXT_DIM = new Color(0x8B, 0x9B, 0xB0);
    private static final Color ACCENT = new Color(0x5C, 0xE1, 0x8B);
    private static final Color FOOD = new Color(0xF2, 0x54, 0x4E);

    private static final Font FONT_HUD = new Font(Font.SANS_SERIF, Font.BOLD, 17);
    private static final Font FONT_SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    private static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 40);
    private static final Font FONT_BIG = new Font(Font.SANS_SERIF, Font.BOLD, 22);

    // ------------------------------------------------------------------
    // Game state
    // ------------------------------------------------------------------

    /** The four directions the snake can travel in. */
    private enum Dir {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final int dx;
        final int dy;

        Dir(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        boolean isOpposite(Dir other) {
            return dx + other.dx == 0 && dy + other.dy == 0;
        }
    }

    private enum State { READY, RUNNING, PAUSED, GAME_OVER }

    private final Deque<Point> snake = new ArrayDeque<>();
    /** Queued key presses, so quick successive turns are not lost. */
    private final Deque<Dir> pendingDirs = new ArrayDeque<>();
    private final Random random = new Random();
    private final Timer timer;

    private Point food;
    private Dir dir;
    private State state = State.READY;
    private int score;
    private int highScore;
    private int ticksAlive;

    public SnakeGame() {
        setPreferredSize(new Dimension(COLS * CELL + 2 * PAD, ROWS * CELL + HUD + PAD));
        setBackground(BG_BOTTOM);
        setFocusable(true);
        highScore = loadHighScore();
        timer = new Timer(BASE_DELAY_MS, this);
        timer.setCoalesce(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e);
            }
        });
        newGame();
    }

    // ------------------------------------------------------------------
    // Setup / reset
    // ------------------------------------------------------------------

    private void newGame() {
        snake.clear();
        pendingDirs.clear();
        int startY = ROWS / 2;
        for (int i = 0; i < INITIAL_LENGTH; i++) {
            snake.addLast(new Point(INITIAL_LENGTH - 1 - i + 2, startY));
        }
        dir = Dir.RIGHT;
        score = 0;
        ticksAlive = 0;
        state = State.READY;
        placeFood();
        timer.setDelay(BASE_DELAY_MS);
        timer.stop();
        repaint();
    }

    private void startOrResume() {
        if (state == State.READY || state == State.PAUSED) {
            state = State.RUNNING;
            timer.start();
        }
    }

    private void togglePause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            timer.stop();
        } else if (state == State.PAUSED) {
            startOrResume();
        }
        repaint();
    }

    /** Places food on a random free cell (assumes the snake is smaller than the board). */
    private void placeFood() {
        if (snake.size() >= COLS * ROWS) {
            return; // board is full - the player has won
        }
        do {
            food = new Point(random.nextInt(COLS), random.nextInt(ROWS));
        } while (snake.contains(food));
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    private void handleKey(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                queueDir(Dir.UP);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                queueDir(Dir.DOWN);
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                queueDir(Dir.LEFT);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                queueDir(Dir.RIGHT);
                break;
            case KeyEvent.VK_SPACE:
                if (state == State.GAME_OVER) {
                    newGame();
                    startOrResume();
                } else {
                    startOrResume();
                }
                break;
            case KeyEvent.VK_P:
                togglePause();
                break;
            case KeyEvent.VK_R:
                newGame();
                break;
            default:
                return;
        }
        if (state == State.READY && !pendingDirs.isEmpty()) {
            startOrResume();
        }
        repaint();
    }

    private void queueDir(Dir next) {
        Dir last = pendingDirs.peekLast();
        Dir reference = last != null ? last : dir;
        if (reference == next || reference.isOpposite(next)) {
            return; // ignore no-ops and 180 degree turns
        }
        if (pendingDirs.size() < 2) {
            pendingDirs.addLast(next);
        }
    }

    // ------------------------------------------------------------------
    // Game loop
    // ------------------------------------------------------------------

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state != State.RUNNING) {
            return;
        }
        step();
        repaint();
    }

    private void step() {
        Dir next = pendingDirs.pollFirst();
        if (next != null && !dir.isOpposite(next)) {
            dir = next;
        }
        ticksAlive++;

        Point head = snake.peekFirst();
        Point newHead = new Point(head.x + dir.dx, head.y + dir.dy);

        if (newHead.x < 0 || newHead.y < 0 || newHead.x >= COLS || newHead.y >= ROWS) {
            gameOver();
            return;
        }

        boolean grows = newHead.equals(food);
        if (hitsBody(newHead, grows)) {
            gameOver();
            return;
        }

        snake.addFirst(newHead);
        if (grows) {
            score++;
            if (score > highScore) {
                highScore = score;
                saveHighScore(highScore);
            }
            placeFood();
            int delay = Math.max(MIN_DELAY_MS, BASE_DELAY_MS - score * SPEEDUP_PER_POINT_MS);
            timer.setDelay(delay);
        } else {
            snake.removeLast();
        }
    }

    /**
     * @param candidate the cell the head wants to move into
     * @param grows     whether the tail stays put on this step (it is only free to move if not)
     */
    private boolean hitsBody(Point candidate, boolean grows) {
        Iterator<Point> it = snake.iterator();
        int index = 0;
        int lastIndex = snake.size() - 1;
        while (it.hasNext()) {
            Point p = it.next();
            if (!grows && index == lastIndex) {
                break; // the tail vacates this cell on the same step
            }
            if (p.equals(candidate)) {
                return true;
            }
            index++;
        }
        return false;
    }

    private void gameOver() {
        timer.stop();
        state = State.GAME_OVER;
        if (score > highScore) {
            highScore = score;
            saveHighScore(highScore);
        }
        repaint();
    }

    // ------------------------------------------------------------------
    // High score persistence
    // ------------------------------------------------------------------

    private static int loadHighScore() {
        try {
            if (Files.exists(HIGH_SCORE_FILE)) {
                String raw = new String(Files.readAllBytes(HIGH_SCORE_FILE), StandardCharsets.UTF_8).trim();
                return Math.max(0, Integer.parseInt(raw));
            }
        } catch (IOException | NumberFormatException ignored) {
            // a broken high score file must never stop the game from starting
        }
        return 0;
    }

    private static void saveHighScore(int value) {
        try {
            Files.write(HIGH_SCORE_FILE, Integer.toString(value).getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // not being able to persist the high score is not fatal
        }
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g2);
        drawField(g2);
        drawFood(g2);
        drawSnake(g2);
        drawHud(g2);
        drawOverlay(g2);

        g2.dispose();
    }

    private void drawBackground(Graphics2D g2) {
        g2.setPaint(new java.awt.GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawField(Graphics2D g2) {
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                g2.setColor(((x + y) % 2 == 0) ? FIELD_A : FIELD_B);
                g2.fillRect(PAD + x * CELL, HUD + y * CELL, CELL, CELL);
            }
        }
        g2.setColor(FIELD_BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(PAD - 1, HUD - 1, COLS * CELL + 1, ROWS * CELL + 1);
    }

    private void drawFood(Graphics2D g2) {
        if (food == null) {
            return;
        }
        // gentle pulse so the food is easy to spot
        double pulse = 0.5 + 0.5 * Math.sin(ticksAlive * 0.25);
        int inset = (int) Math.round(4 - pulse * 1.5);
        int x = PAD + food.x * CELL + inset;
        int y = HUD + food.y * CELL + inset;
        int size = CELL - 2 * inset;

        g2.setColor(new Color(0xF2, 0x54, 0x4E, 60));
        g2.fillOval(x - 3, y - 3, size + 6, size + 6);
        g2.setColor(FOOD);
        g2.fillOval(x, y, size, size);
        g2.setColor(new Color(0xFF, 0xFF, 0xFF, 140));
        g2.fillOval(x + size / 5, y + size / 6, Math.max(2, size / 4), Math.max(2, size / 4));
        // stem
        g2.setColor(new Color(0x6B, 0xC2, 0x6B));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(x + size / 2, y + 1, x + size / 2, y - 4);
    }

    private void drawSnake(Graphics2D g2) {
        int total = Math.max(1, snake.size());
        int i = 0;
        for (Point p : snake) {
            float t = (float) i / total;                    // 0 at the head, 1 at the tail
            Color body = Color.getHSBColor(0.36f + 0.09f * t, 0.72f - 0.18f * t, 1.0f - 0.42f * t);
            int x = PAD + p.x * CELL;
            int y = HUD + p.y * CELL;

            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRoundRect(x + 2, y + 3, CELL - 3, CELL - 3, 12, 12);
            g2.setColor(body);
            g2.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 12, 12);
            g2.setColor(new Color(255, 255, 255, i == 0 ? 90 : 35));
            g2.fillRoundRect(x + 4, y + 4, CELL - 12, CELL - 14, 8, 8);

            if (i == 0) {
                drawEyes(g2, x, y, body);
            }
            i++;
        }
    }

    private void drawEyes(Graphics2D g2, int x, int y, Color body) {
        int eye = Math.max(3, CELL / 6);
        int aX;
        int aY;
        int bX;
        int bY;
        switch (dir) {
            case UP:
                aX = x + CELL / 4;
                aY = y + CELL / 4;
                bX = x + CELL * 3 / 4 - eye;
                bY = y + CELL / 4;
                break;
            case DOWN:
                aX = x + CELL / 4;
                aY = y + CELL * 3 / 4 - eye;
                bX = x + CELL * 3 / 4 - eye;
                bY = y + CELL * 3 / 4 - eye;
                break;
            case LEFT:
                aX = x + CELL / 4;
                aY = y + CELL / 4;
                bX = x + CELL / 4;
                bY = y + CELL * 3 / 4 - eye;
                break;
            default: // RIGHT
                aX = x + CELL * 3 / 4 - eye;
                aY = y + CELL / 4;
                bX = x + CELL * 3 / 4 - eye;
                bY = y + CELL * 3 / 4 - eye;
                break;
        }
        g2.setColor(Color.WHITE);
        g2.fillOval(aX, aY, eye, eye);
        g2.fillOval(bX, bY, eye, eye);
        g2.setColor(body.darker().darker());
        int pupil = Math.max(2, eye / 2);
        g2.fillOval(aX + eye / 4, aY + eye / 4, pupil, pupil);
        g2.fillOval(bX + eye / 4, bY + eye / 4, pupil, pupil);
    }

    private void drawHud(Graphics2D g2) {
        g2.setFont(FONT_HUD);
        g2.setColor(TEXT);
        g2.drawString("SCORE  " + score, PAD, 30);

        g2.setFont(FONT_HUD);
        g2.setColor(ACCENT);
        String best = "BEST  " + highScore;
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(best);
        g2.drawString(best, getWidth() - PAD - w, 30);

        g2.setFont(FONT_SMALL);
        g2.setColor(TEXT_DIM);
        String length = "len " + snake.size();
        int lw = g2.getFontMetrics().stringWidth(length);
        g2.drawString(length, (getWidth() - lw) / 2, 30);
    }

    private void drawOverlay(Graphics2D g2) {
        if (state == State.RUNNING) {
            return;
        }
        int top = HUD;
        int height = ROWS * CELL;

        g2.setColor(new Color(4, 7, 12, 190));
        g2.fillRect(PAD, top, COLS * CELL, height);

        String title;
        String line1;
        String line2;
        switch (state) {
            case READY:
                title = "SNAKE";
                line1 = "Arrow keys or WASD to move";
                line2 = "Press SPACE to start";
                break;
            case PAUSED:
                title = "PAUSED";
                line1 = "Press P or SPACE to resume";
                line2 = "R restarts the game";
                break;
            default:
                title = "GAME OVER";
                line1 = "Score  " + score + "    Length  " + snake.size();
                line2 = "Press R or SPACE to play again";
                break;
        }

        int centerY = top + height / 2;
        g2.setFont(FONT_TITLE);
        g2.setColor(state == State.GAME_OVER ? FOOD : TEXT);
        drawCentered(g2, title, centerY - 34);

        g2.setFont(FONT_BIG);
        g2.setColor(TEXT);
        drawCentered(g2, line1, centerY + 22);

        g2.setFont(FONT_SMALL);
        g2.setColor(TEXT_DIM);
        drawCentered(g2, line2, centerY + 52);
    }

    private void drawCentered(Graphics2D g2, String text, int baselineY) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, baselineY);
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // the cross-platform look and feel is a fine fallback
            }
            JFrame frame = new JFrame("Snake - Java Swing");
            SnakeGame game = new SnakeGame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game);
            frame.setResizable(false);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
