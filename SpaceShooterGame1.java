import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Iterator;

public class SpaceShooterGame1 extends JPanel implements ActionListener, KeyListener {

    // Window dimensions
    static final int WIDTH  = 800;
    static final int HEIGHT = 600;

    // Game state flags
    boolean gameOver = false;
    boolean gameWon  = false;

    // Score
    int score = 0;

    // Player
    int playerX = WIDTH / 2 - 25;
    int playerY = HEIGHT - 80;
    int playerWidth  = 50;
    int playerHeight = 40;
    int playerSpeed  = 6;

    // Movement flags
    boolean movingLeft  = false;
    boolean movingRight = false;
    boolean movingUp    = false;
    boolean movingDown  = false;

    // Shooting cooldown
    int shootCooldown = 0;

    // Projectiles
    ArrayList<Rectangle> bullets = new ArrayList<>();
    int bulletSpeed = 10;

    // Enemies
    ArrayList<Rectangle> enemies    = new ArrayList<>();
    ArrayList<Integer>   enemyDX    = new ArrayList<>();
    ArrayList<Integer>   enemyDY    = new ArrayList<>();
    ArrayList<Integer>   enemyTimer = new ArrayList<>();
    int totalEnemies = 10;

    // Game loop timer (60 fps)
    Timer timer;

    // Stars for background
    int[][] stars = new int[80][2];

    public SpaceShooterGame1() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        // Scatter background stars
        for (int i = 0; i < stars.length; i++) {
            stars[i][0] = (int)(Math.random() * WIDTH);
            stars[i][1] = (int)(Math.random() * HEIGHT);
        }

        spawnEnemies();

        timer = new Timer(16, this);
        timer.start();
    }

    // -------------------------------------------------------
    //  Spawn 10 enemies in two rows
    // -------------------------------------------------------
    void spawnEnemies() {
        int cols = 5;
        int rows = 2;
        int eW = 50, eH = 30;
        int xGap = (WIDTH - cols * eW) / (cols + 1);
        int yStart = 60;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = xGap + c * (eW + xGap);
                int y = yStart + r * 80;
                enemies.add(new Rectangle(x, y, eW, eH));
                enemyDX.add(randomSpeed());
                enemyDY.add(randomSpeed());
                enemyTimer.add(randomInterval());
            }
        }
    }

    int randomSpeed() {
        int[] speeds = {-4, -3, -2, 2, 3, 4};
        return speeds[(int)(Math.random() * speeds.length)];
    }

    int randomInterval() {
        return 20 + (int)(Math.random() * 40); // change direction every 20-60 frames
    }

    // -------------------------------------------------------
    //  Game loop
    // -------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !gameWon) {
            update();
        }
        repaint();
    }

    void update() {
        // Move player
        if (movingLeft  && playerX > 0)                        playerX -= playerSpeed;
        if (movingRight && playerX < WIDTH - playerWidth)       playerX += playerSpeed;
        if (movingUp    && playerY > HEIGHT / 2)               playerY -= playerSpeed;
        if (movingDown  && playerY < HEIGHT - playerHeight - 5) playerY += playerSpeed;

        // Auto-fire every 20 frames
        if (shootCooldown > 0) {
            shootCooldown--;
        } else {
            bullets.add(new Rectangle(playerX + playerWidth / 2 - 3, playerY - 10, 6, 18));
            shootCooldown = 20;
        }

        // Move bullets
        Iterator<Rectangle> bi = bullets.iterator();
        while (bi.hasNext()) {
            Rectangle b = bi.next();
            b.y -= bulletSpeed;
            if (b.y < 0) bi.remove();
        }

        // Move enemies sporadically — each has its own velocity and direction-change timer
        for (int i = 0; i < enemies.size(); i++) {
            Rectangle en = enemies.get(i);

            // Count down timer; on expiry pick a new random direction
            int t = enemyTimer.get(i) - 1;
            if (t <= 0) {
                enemyDX.set(i, randomSpeed());
                enemyDY.set(i, randomSpeed());
                t = randomInterval();
            }
            enemyTimer.set(i, t);

            // Apply velocity
            en.x += enemyDX.get(i);
            en.y += enemyDY.get(i);

            // Bounce off walls and top; never go below player's upper boundary
            if (en.x < 0) { en.x = 0; enemyDX.set(i, Math.abs(enemyDX.get(i))); }
            if (en.x + en.width > WIDTH) { en.x = WIDTH - en.width; enemyDX.set(i, -Math.abs(enemyDX.get(i))); }
            if (en.y < 0) { en.y = 0; enemyDY.set(i, Math.abs(enemyDY.get(i))); }
            if (en.y + en.height > playerY - 10) { en.y = playerY - 10 - en.height; enemyDY.set(i, -Math.abs(enemyDY.get(i))); }
        }

        // Bullet vs enemy collision
        Iterator<Rectangle> bIter = bullets.iterator();
        while (bIter.hasNext()) {
            Rectangle b = bIter.next();
            boolean hit = false;
            for (int i = 0; i < enemies.size(); i++) {
                if (b.intersects(enemies.get(i))) {
                    enemies.remove(i);
                    enemyDX.remove(i);
                    enemyDY.remove(i);
                    enemyTimer.remove(i);
                    score++;
                    hit = true;
                    break;
                }
            }
            if (hit) bIter.remove();
        }

        // Win condition
        if (enemies.isEmpty()) {
            gameWon = true;
            timer.stop();
        }

        // Lose condition — enemy reaches player level
        for (Rectangle en : enemies) {
            if (en.y + en.height >= playerY) {
                gameOver = true;
                timer.stop();
                break;
            }
        }
    }

    // -------------------------------------------------------
    //  Rendering
    // -------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Stars
        g2.setColor(Color.WHITE);
        for (int[] star : stars) {
            g2.fillOval(star[0], star[1], 2, 2);
        }

        if (gameWon) {
            drawCenteredMessage(g2, "YOU WIN!", "Score: " + score + " / " + totalEnemies, new Color(50, 220, 50));
            return;
        }

        if (gameOver) {
            drawCenteredMessage(g2, "GAME OVER", "Score: " + score + " / " + totalEnemies, new Color(220, 50, 50));
            return;
        }

        // Player ship (triangle body + wings)
        drawPlayer(g2);

        // Bullets
        g2.setColor(new Color(100, 220, 255));
        for (Rectangle b : bullets) {
            g2.fillRoundRect(b.x, b.y, b.width, b.height, 4, 4);
        }

        // Enemies
        for (Rectangle en : enemies) {
            drawEnemy(g2, en);
        }

        // HUD
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2.drawString("SCORE: " + score, 15, 25);
        g2.drawString("ENEMIES: " + enemies.size() + " LEFT", WIDTH - 200, 25);

        // Controls reminder
        g2.setColor(new Color(150, 150, 150));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.drawString("ARROW KEYS: Move   SPACE: Shoot", WIDTH / 2 - 150, HEIGHT - 10);
    }

    void drawPlayer(Graphics2D g2) {
        // Main body
        int[] xPts = { playerX + playerWidth / 2, playerX, playerX + playerWidth };
        int[] yPts = { playerY, playerY + playerHeight, playerY + playerHeight };
        g2.setColor(new Color(60, 180, 255));
        g2.fillPolygon(xPts, yPts, 3);

        // Cockpit
        g2.setColor(new Color(180, 230, 255));
        g2.fillOval(playerX + playerWidth / 2 - 8, playerY + 8, 16, 16);

        // Engine glow
        g2.setColor(new Color(255, 140, 0, 180));
        g2.fillOval(playerX + playerWidth / 2 - 6, playerY + playerHeight - 4, 12, 12);
    }

    void drawEnemy(Graphics2D g2, Rectangle en) {
        // Enemy body (saucer shape)
        g2.setColor(new Color(220, 50, 80));
        g2.fillRoundRect(en.x, en.y + en.height / 3, en.width, en.height * 2 / 3, 10, 10);

        // Dome
        g2.setColor(new Color(255, 100, 120));
        g2.fillOval(en.x + en.width / 4, en.y, en.width / 2, en.height / 2 + 4);

        // Eyes
        g2.setColor(Color.YELLOW);
        g2.fillOval(en.x + 10, en.y + en.height / 3 + 4, 7, 7);
        g2.fillOval(en.x + en.width - 17, en.y + en.height / 3 + 4, 7, 7);
    }

    void drawCenteredMessage(Graphics2D g2, String title, String sub, Color color) {
        // Dim overlay
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(color);
        g2.setFont(new Font("Monospaced", Font.BOLD, 52));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, (WIDTH - fm.stringWidth(title)) / 2, HEIGHT / 2 - 30);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 22));
        fm = g2.getFontMetrics();
        g2.drawString(sub, (WIDTH - fm.stringWidth(sub)) / 2, HEIGHT / 2 + 20);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        String restart = "Close and re-run to play again";
        fm = g2.getFontMetrics();
        g2.setColor(new Color(180, 180, 180));
        g2.drawString(restart, (WIDTH - fm.stringWidth(restart)) / 2, HEIGHT / 2 + 60);
    }

    // -------------------------------------------------------
    //  Key handling
    // -------------------------------------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT)  movingLeft  = true;
        if (k == KeyEvent.VK_RIGHT) movingRight = true;
        if (k == KeyEvent.VK_UP)    movingUp    = true;
        if (k == KeyEvent.VK_DOWN)  movingDown  = true;
        if (k == KeyEvent.VK_SPACE) {
            // Manual shoot also resets cooldown for instant fire
            if (shootCooldown == 0) {
                bullets.add(new Rectangle(playerX + playerWidth / 2 - 3, playerY - 10, 6, 18));
                shootCooldown = 20;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT)  movingLeft  = false;
        if (k == KeyEvent.VK_RIGHT) movingRight = false;
        if (k == KeyEvent.VK_UP)    movingUp    = false;
        if (k == KeyEvent.VK_DOWN)  movingDown  = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    // -------------------------------------------------------
    //  Entry point
    // -------------------------------------------------------
    public static void main(String[] args) {
        JFrame frame = new JFrame("Space Shooter");
        SpaceShooterGame1 game = new SpaceShooterGame1();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}