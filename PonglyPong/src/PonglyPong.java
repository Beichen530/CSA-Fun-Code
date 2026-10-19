import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class PonglyPong extends JPanel implements ActionListener, KeyListener {
    private static final int SIZE = 750;
    private static final Point CENTER = new Point(SIZE / 2, SIZE / 2);

    // Dynamic Arena Variables
    private float arenaAngle = 0.0f;
    private double currentRadius = 300.0;
    private final double minRadius = 120.0;
    private final double shrinkRate = 0.02;

    // Dynamic Vertices
    private Point pLeft = new Point();
    private Point pRight = new Point();
    private Point pBottom = new Point();

    // Player Health
    private int hp1 = 10, hp2 = 10, hp3 = 10;

    // AI Toggle States (Keys 1, 2, 3)
    private boolean ai1 = false, ai2 = false, ai3 = false;

    // Control Inversion Toggles (Keys 4, 5, 6) - Default is NO flipping
    private boolean invert1 = false, invert2 = false, invert3 = false;

    // Screen Warnings
    private String warningText = "";
    private int warningTimer = 0;

    // Inputs
    private boolean wPressed, sPressed, upPressed, downPressed, num7Pressed, num8Pressed;

    // Entities & Effects
    private ArrayList<Ball> balls = new ArrayList<>();
    private ArrayList<Particle> particles = new ArrayList<>();
    private ArrayList<PowerUp> powerUps = new ArrayList<>();

    // Paddle Positions
    private float pos1 = 0.5f, pos2 = 0.5f, pos3 = 0.5f;

    // Visual Juice Effects
    private int screenShakeX = 0, screenShakeY = 0, shakeDuration = 0;
    private Color flashColor = null;
    private int flashTimer = 0;
    private Random random = new Random();
    private int powerUpTimer = 0;
    private String winnerText = null;

    public PonglyPong() {
        setPreferredSize(new Dimension(SIZE, SIZE));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        updateVertices();
        spawnBall();

        Timer timer = new Timer(16, this);
        timer.start();
    }

    private void updateVertices() {
        double aLeft = Math.PI * 7 / 6 + arenaAngle;
        double aRight = Math.PI * 11 / 6 + arenaAngle;
        double aBottom = Math.PI / 2 + arenaAngle;

        pLeft.setLocation(CENTER.x + currentRadius * Math.cos(aLeft), CENTER.y + currentRadius * Math.sin(aLeft));
        pRight.setLocation(CENTER.x + currentRadius * Math.cos(aRight), CENTER.y + currentRadius * Math.sin(aRight));
        pBottom.setLocation(CENTER.x + currentRadius * Math.cos(aBottom), CENTER.y + currentRadius * Math.sin(aBottom));
    }

    private void spawnBall() {
        Ball b = new Ball(CENTER.x, CENTER.y);
        double angle = random.nextDouble() * Math.PI * 2;
        float speed = 6.5f;
        b.vx = (float) Math.cos(angle) * speed;
        b.vy = (float) Math.sin(angle) * speed;
        balls.add(b);
    }

    private void triggerShake(int intensity) {
        shakeDuration = Math.max(shakeDuration, intensity);
    }

    private void triggerFlash(Color color, int duration) {
        flashColor = color;
        flashTimer = duration;
    }

    private void setWarning(String msg) {
        warningText = msg;
        warningTimer = 60; // 1 second display
    }

    private void createExplosion(float x, float y, Color color, int count, float maxSpeed) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color, random, maxSpeed));
        }
    }

    private float updateAI(Point lineA, Point lineB, float currentPos) {
        if (balls.isEmpty()) return currentPos;

        Ball targetBall = balls.get(0);
        double minDist = Double.MAX_VALUE;
        for (Ball b : balls) {
            double d = Math.hypot(b.x - CENTER.x, b.y - CENTER.y);
            if (d < minDist) {
                minDist = d;
                targetBall = b;
            }
        }

        double dx = lineB.x - lineA.x;
        double dy = lineB.y - lineA.y;
        double lineLengthSq = dx * dx + dy * dy;

        double targetT = ((targetBall.x - lineA.x) * dx + (targetBall.y - lineA.y) * dy) / lineLengthSq;
        targetT = Math.max(0.0f, Math.min(1.0f, targetT));

        float aiSpeed = 0.045f;
        if (currentPos < targetT) {
            return (float) Math.min(targetT, currentPos + aiSpeed);
        } else if (currentPos > targetT) {
            return (float) Math.max(targetT, currentPos - aiSpeed);
        }
        return currentPos;
    }

    private float processMovement(Point pA, Point pB, float currentPos, boolean primaryPressed, boolean secondaryPressed, boolean shouldInvert, float moveSpeed) {
        if (!primaryPressed && !secondaryPressed) return currentPos;

        boolean moveTowardsB = primaryPressed;
        boolean moveTowardsA = secondaryPressed;

        if (shouldInvert) {
            double dy = pB.y - pA.y;
            if (dy < 0) { // Vertex B is higher than Vertex A on screen
                moveTowardsB = secondaryPressed;
                moveTowardsA = primaryPressed;
            }
        }

        if (moveTowardsB) currentPos += moveSpeed;
        if (moveTowardsA) currentPos -= moveSpeed;

        return Math.max(0.0f, Math.min(1.0f, currentPos));
    }

    private void checkWinCondition() {
        int aliveCount = 0;
        if (hp1 > 0) aliveCount++;
        if (hp2 > 0) aliveCount++;
        if (hp3 > 0) aliveCount++;

        if (aliveCount <= 1) {
            if (hp1 > 0) winnerText = "BLUE WINS!";
            else if (hp2 > 0) winnerText = "PINK WINS!";
            else if (hp3 > 0) winnerText = "YELLOW WINS!";
            else winnerText = "GAME OVER";
        }
    }

    private boolean checkSolidWallBounce(Ball b, Point pA, Point pB) {
        double dist = distToSegment(b.x, b.y, pA, pB);
        if (dist < 18) {
            double nx = -(pB.y - pA.y);
            double ny = pB.x - pA.x;
            double len = Math.hypot(nx, ny);
            if (len == 0) return false;

            nx /= len; ny /= len;

            double toCenterX = CENTER.x - b.x;
            double toCenterY = CENTER.y - b.y;
            if (nx * toCenterX + ny * toCenterY < 0) {
                nx = -nx; ny = -ny;
            }

            double dot = b.vx * nx + b.vy * ny;
            if (dot < 0) {
                b.vx = (float) (b.vx - 2 * dot * nx);
                b.vy = (float) (b.vy - 2 * dot * ny);
                b.x += (float) (nx * 4);
                b.y += (float) (ny * 4);
                return true;
            }
        }
        return false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (winnerText != null) {
            repaint();
            return;
        }

        // 0. Transformation & Timers
        arenaAngle += 0.005f;
        if (currentRadius > minRadius) currentRadius -= shrinkRate;
        updateVertices();

        if (warningTimer > 0) warningTimer--;
        if (flashTimer > 0) flashTimer--;

        // 1. Controls
        float moveSpeed = 0.04f;

        if (hp1 > 0) {
            if (ai1) pos1 = updateAI(pBottom, pLeft, pos1);
            else pos1 = processMovement(pBottom, pLeft, pos1, wPressed, sPressed, invert1, moveSpeed);
        }

        if (hp2 > 0) {
            if (ai2) pos2 = updateAI(pBottom, pRight, pos2);
            else pos2 = processMovement(pBottom, pRight, pos2, upPressed, downPressed, invert2, moveSpeed);
        }

        if (hp3 > 0) {
            if (ai3) pos3 = updateAI(pLeft, pRight, pos3);
            else pos3 = processMovement(pLeft, pRight, pos3, num7Pressed, num8Pressed, invert3, moveSpeed);
        }

        // 2. Shake Decay
        if (shakeDuration > 0) {
            screenShakeX = random.nextInt(shakeDuration + 1) - (shakeDuration / 2);
            screenShakeY = random.nextInt(shakeDuration + 1) - (shakeDuration / 2);
            shakeDuration--;
        } else {
            screenShakeX = 0; screenShakeY = 0;
        }

        // 3. Power-Up Spawning
        powerUpTimer++;
        if (powerUpTimer > 280) {
            powerUpTimer = 0;
            if (powerUps.size() < 2) {
                float px = CENTER.x + (random.nextFloat() - 0.5f) * (float) (currentRadius * 0.7);
                float py = CENTER.y + (random.nextFloat() - 0.5f) * (float) (currentRadius * 0.7);
                powerUps.add(new PowerUp(px, py, random.nextInt(2)));
            }
        }

        // 4. Particles Update
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.update();
            if (p.isDead()) pIter.remove();
        }

        // 5. Balls Physics & Collisions
        ArrayList<Ball> ballsToSpawn = new ArrayList<>();

        Iterator<Ball> bIter = balls.iterator();
        while (bIter.hasNext()) {
            Ball b = bIter.next();

            // Trail Particles
            if (random.nextInt(2) == 0) {
                particles.add(new Particle(b.x, b.y, new Color(255, 255, 255, 120), random, 1.5f));
            }

            float dx = CENTER.x - b.x;
            float dy = CENTER.y - b.y;
            float dist = (float) Math.hypot(dx, dy);

            // Black Hole Logic
            if (dist < 25) {
                double launchAngle = random.nextDouble() * Math.PI * 2;
                float launchSpeed = 12.0f;
                b.vx = (float) Math.cos(launchAngle) * launchSpeed;
                b.vy = (float) Math.sin(launchAngle) * launchSpeed;
                triggerShake(18);
                triggerFlash(new Color(180, 0, 255, 80), 6);
                createExplosion(CENTER.x, CENTER.y, new Color(180, 0, 255), 40, 8.0f);
            } else if (dist < currentRadius * 0.75) {
                float pullForce = 0.28f;
                b.vx += (dx / dist) * pullForce + (-dy / dist) * 0.18f;
                b.vy += (dy / dist) * pullForce + (dx / dist) * 0.18f;
            }

            b.move();

            // PowerUp Collisions
            Iterator<PowerUp> powIter = powerUps.iterator();
            while (powIter.hasNext()) {
                PowerUp pow = powIter.next();
                if (Math.hypot(b.x - pow.x, b.y - pow.y) < 24) {
                    if (pow.type == 0) {
                        Ball newBall = new Ball(CENTER.x, CENTER.y);
                        double angle = random.nextDouble() * Math.PI * 2;
                        newBall.vx = (float) Math.cos(angle) * 7.0f;
                        newBall.vy = (float) Math.sin(angle) * 7.0f;
                        ballsToSpawn.add(newBall);
                        createExplosion(pow.x, pow.y, Color.ORANGE, 30, 6.0f);
                    } else {
                        if (hp1 > 0 && hp1 < 10) hp1++;
                        if (hp2 > 0 && hp2 < 10) hp2++;
                        if (hp3 > 0 && hp3 < 10) hp3++;
                        createExplosion(pow.x, pow.y, Color.GREEN, 30, 6.0f);
                    }
                    powIter.remove();
                }
            }

            // Dead Barrier Rebound
            boolean hitSolidWall = false;
            if (hp1 <= 0 && checkSolidWallBounce(b, pBottom, pLeft)) hitSolidWall = true;
            if (hp2 <= 0 && checkSolidWallBounce(b, pBottom, pRight)) hitSolidWall = true;
            if (hp3 <= 0 && checkSolidWallBounce(b, pLeft, pRight)) hitSolidWall = true;

            if (hitSolidWall) {
                triggerShake(8);
                createExplosion(b.x, b.y, Color.RED, 15, 4.0f);
                continue;
            }

            // Paddle Collisions
            Point2D paddle1Loc = getPointOnLine(pBottom, pLeft, pos1);
            Point2D paddle2Loc = getPointOnLine(pBottom, pRight, pos2);
            Point2D paddle3Loc = getPointOnLine(pLeft, pRight, pos3);

            if (hp1 > 0 && distToPoint(b.x, b.y, paddle1Loc) < 35 && b.cooldown <= 0) {
                b.bounce(pos1);
                triggerShake(12);
                triggerFlash(new Color(0, 255, 255, 60), 4);
                createExplosion(b.x, b.y, Color.CYAN, 25, 7.0f);
                b.cooldown = 8;
            } else if (hp2 > 0 && distToPoint(b.x, b.y, paddle2Loc) < 35 && b.cooldown <= 0) {
                b.bounce(pos2);
                triggerShake(12);
                triggerFlash(new Color(255, 0, 255, 60), 4);
                createExplosion(b.x, b.y, Color.MAGENTA, 25, 7.0f);
                b.cooldown = 8;
            } else if (hp3 > 0 && distToPoint(b.x, b.y, paddle3Loc) < 35 && b.cooldown <= 0) {
                b.bounce(pos3);
                triggerShake(14);
                triggerFlash(new Color(255, 255, 0, 60), 4);
                createExplosion(b.x, b.y, Color.YELLOW, 30, 8.0f);
                b.cooldown = 8;
            }

            // Damage / Out Of Bounds
            if (dist > currentRadius + 30) {
                double d1 = distToSegment(b.x, b.y, pBottom, pLeft);
                double d2 = distToSegment(b.x, b.y, pBottom, pRight);
                double d3 = distToSegment(b.x, b.y, pLeft, pRight);

                if (d1 <= d2 && d1 <= d3 && hp1 > 0) { hp1--; checkWinCondition(); }
                else if (d2 <= d1 && d2 <= d3 && hp2 > 0) { hp2--; checkWinCondition(); }
                else if (hp3 > 0) { hp3--; checkWinCondition(); }

                triggerShake(22);
                triggerFlash(new Color(255, 0, 0, 100), 8);
                createExplosion(b.x, b.y, Color.RED, 45, 9.0f);
                bIter.remove();
            }
        }

        balls.addAll(ballsToSpawn);
        if (balls.isEmpty() && winnerText == null) spawnBall();

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.translate(screenShakeX, screenShakeY);

        // Background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, SIZE, SIZE);

        // Flash Impact Effect
        if (flashTimer > 0 && flashColor != null) {
            g2d.setColor(flashColor);
            g2d.fillRect(0, 0, SIZE, SIZE);
        }

        // Center Black Hole
        g2d.setColor(new Color(150, 0, 255, 140));
        g2d.fillOval(CENTER.x - 38, CENTER.y - 38, 76, 76);
        g2d.setColor(Color.BLACK);
        g2d.fillOval(CENTER.x - 20, CENTER.y - 20, 40, 40);
        g2d.setColor(Color.WHITE);
        g2d.drawOval(CENTER.x - 20, CENTER.y - 20, 40, 40);

        // Walls
        drawWallSegment(g2d, pBottom, pLeft, hp1 > 0 ? Color.CYAN : new Color(220, 20, 60), hp1 <= 0);
        drawWallSegment(g2d, pBottom, pRight, hp2 > 0 ? Color.MAGENTA : new Color(220, 20, 60), hp2 <= 0);
        drawWallSegment(g2d, pLeft, pRight, hp3 > 0 ? Color.YELLOW : new Color(220, 20, 60), hp3 <= 0);

        // Power-Ups
        for (PowerUp pow : powerUps) {
            g2d.setColor(pow.type == 0 ? Color.ORANGE : Color.GREEN);
            g2d.fillOval((int) pow.x - 12, (int) pow.y - 12, 24, 24);
            g2d.setColor(Color.WHITE);
            g2d.drawOval((int) pow.x - 12, (int) pow.y - 12, 24, 24);
        }

        // Active Paddles
        if (hp1 > 0) drawPaddle(g2d, pBottom, pLeft, pos1, Color.CYAN);
        if (hp2 > 0) drawPaddle(g2d, pBottom, pRight, pos2, Color.MAGENTA);
        if (hp3 > 0) drawPaddle(g2d, pLeft, pRight, pos3, Color.YELLOW);

        // Particles & Balls
        for (Particle p : particles) p.draw(g2d);
        for (Ball b : balls) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval((int) b.x - 9, (int) b.y - 9, 18, 18);
        }

        // HUD Text
        g2d.setFont(new Font("Consolas", Font.BOLD, 17));
        g2d.setColor(hp1 > 0 ? Color.CYAN : Color.GRAY);
        g2d.drawString("P1 Blue [W/S]" + (ai1 ? " (BOT)" : "") + (invert1 ? " [FLIP]" : "") + ": " + (hp1 > 0 ? hp1 : "OUT"), 15, 30);

        g2d.setColor(hp2 > 0 ? Color.MAGENTA : Color.GRAY);
        g2d.drawString("P2 Pink [UP/DN]" + (ai2 ? " (BOT)" : "") + (invert2 ? " [FLIP]" : "") + ": " + (hp2 > 0 ? hp2 : "OUT"), SIZE - 280, 30);

        g2d.setColor(hp3 > 0 ? Color.YELLOW : Color.GRAY);
        g2d.drawString("P3 Yellow [7/8]" + (ai3 ? " (BOT)" : "") + (invert3 ? " [FLIP]" : "") + ": " + (hp3 > 0 ? hp3 : "OUT"), SIZE / 2 - 130, SIZE - 40);

        // Controls Bar
        g2d.setFont(new Font("Consolas", Font.PLAIN, 12));
        g2d.setColor(Color.GRAY);
        g2d.drawString("Bots: 1=P1 | 2=P2 | 3=P3  ||  Toggle Control Flip: 4=P1 | 5=P2 | 6=P3", 15, SIZE - 15);

        // On-Screen Inversion Warning Banner
        if (warningTimer > 0) {
            g2d.setFont(new Font("Consolas", Font.BOLD, 36));
            g2d.setColor(Color.RED);
            FontMetrics fm = g2d.getFontMetrics();
            int tx = (SIZE - fm.stringWidth(warningText)) / 2;
            g2d.drawString(warningText, tx, SIZE / 2 + 80);
        }

        // Winner Overlay
        if (winnerText != null) {
            g2d.setFont(new Font("Consolas", Font.BOLD, 52));
            g2d.setColor(Color.GREEN);
            FontMetrics fm = g2d.getFontMetrics();
            int tx = (SIZE - fm.stringWidth(winnerText)) / 2;
            g2d.drawString(winnerText, tx, SIZE / 2 - 50);
        }

        g2d.dispose();
    }

    private void drawWallSegment(Graphics2D g2d, Point pA, Point pB, Color color, boolean isSolid) {
        g2d.setStroke(new BasicStroke(isSolid ? 8 : 4));
        g2d.setColor(color);
        g2d.drawLine(pA.x, pA.y, pB.x, pB.y);
    }

    private void drawPaddle(Graphics2D g2d, Point a, Point b, float pos, Color color) {
        Point2D loc = getPointOnLine(a, b, pos);
        g2d.setColor(color);
        g2d.fillOval((int) loc.getX() - 22, (int) loc.getY() - 22, 44, 44);
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval((int) loc.getX() - 22, (int) loc.getY() - 22, 44, 44);
    }

    private Point2D getPointOnLine(Point a, Point b, float t) {
        return new Point2D.Double(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y));
    }

    private double distToPoint(float x, float y, Point2D p) {
        return Math.hypot(x - p.getX(), y - p.getY());
    }

    private double distToSegment(float x, float y, Point p1, Point p2) {
        double l2 = Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2);
        if (l2 == 0) return Math.hypot(x - p1.x, y - p1.y);
        double t = Math.max(0, Math.min(1, ((x - p1.x) * (p2.x - p1.x) + (y - p1.y) * (p2.y - p1.y)) / l2));
        Point2D proj = getPointOnLine(p1, p2, (float) t);
        return Math.hypot(x - proj.getX(), y - proj.getY());
    }

    // Key Listeners
    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_W) wPressed = true;
        if (k == KeyEvent.VK_S) sPressed = true;
        if (k == KeyEvent.VK_UP) upPressed = true;
        if (k == KeyEvent.VK_DOWN) downPressed = true;
        if (k == KeyEvent.VK_7 || k == KeyEvent.VK_NUMPAD7) num7Pressed = true;
        if (k == KeyEvent.VK_8 || k == KeyEvent.VK_NUMPAD8) num8Pressed = true;

        // Toggle AI
        if (k == KeyEvent.VK_1 || k == KeyEvent.VK_NUMPAD1) ai1 = !ai1;
        if (k == KeyEvent.VK_2 || k == KeyEvent.VK_NUMPAD2) ai2 = !ai2;
        if (k == KeyEvent.VK_3 || k == KeyEvent.VK_NUMPAD3) ai3 = !ai3;

        // Toggle Inversion with Warnings
        if (k == KeyEvent.VK_4 || k == KeyEvent.VK_NUMPAD4) {
            invert1 = !invert1;
            setWarning(invert1 ? "P1 FLIPPED!" : "P1 NORMAL");
        }
        if (k == KeyEvent.VK_5 || k == KeyEvent.VK_NUMPAD5) {
            invert2 = !invert2;
            setWarning(invert2 ? "P2 FLIPPED!" : "P2 NORMAL");
        }
        if (k == KeyEvent.VK_6 || k == KeyEvent.VK_NUMPAD6) {
            invert3 = !invert3;
            setWarning(invert3 ? "P3 FLIPPED!" : "P3 NORMAL");
        }
    }

    @Override public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_W) wPressed = false;
        if (k == KeyEvent.VK_S) sPressed = false;
        if (k == KeyEvent.VK_UP) upPressed = false;
        if (k == KeyEvent.VK_DOWN) downPressed = false;
        if (k == KeyEvent.VK_7 || k == KeyEvent.VK_NUMPAD7) num7Pressed = false;
        if (k == KeyEvent.VK_8 || k == KeyEvent.VK_NUMPAD8) num8Pressed = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    // Inner Classes
    private static class Ball {
        float x, y, vx, vy;
        int cooldown = 0;

        Ball(float x, float y) { this.x = x; this.y = y; }

        void move() {
            x += vx; y += vy;
            if (cooldown > 0) cooldown--;

            float speed = (float) Math.hypot(vx, vy);
            if (speed > 15.0f) {
                vx = (vx / speed) * 15.0f;
                vy = (vy / speed) * 15.0f;
            }
        }

        void bounce(float paddlePos) {
            vx = -vx * 1.12f + (paddlePos - 0.5f) * 2.5f;
            vy = -vy * 1.12f;
        }
    }

    private static class PowerUp {
        float x, y;
        int type;

        PowerUp(float x, float y, int type) {
            this.x = x; this.y = y; this.type = type;
        }
    }

    private static class Particle {
        float x, y, vx, vy;
        int life;
        Color color;

        Particle(float x, float y, Color color, Random rng, float maxSpeed) {
            this.x = x; this.y = y; this.color = color;
            this.life = 15 + rng.nextInt(15);
            float angle = rng.nextFloat() * (float) Math.PI * 2;
            float speed = 1.0f + rng.nextFloat() * maxSpeed;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
        }

        void update() { x += vx; y += vy; life--; }
        boolean isDead() { return life <= 0; }

        void draw(Graphics2D g2d) {
            g2d.setColor(color);
            g2d.fillRect((int) x, (int) y, 4, 4);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("PonglyPong: Juicy Edition");
        PonglyPong game = new PonglyPong();
        frame.add(game);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}