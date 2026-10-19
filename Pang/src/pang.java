import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class pang extends JPanel implements ActionListener, KeyListener {
    private final Timer timer = new Timer(16, this);

    // Player State
    private double playerY = 250;
    private double playerX = 120;
    private final double fieldTop = 120;
    private final double fieldBottom = 420;
    private final double baseMoveSpeed = 5.8;

    // Control Inputs
    private boolean upPressed, downPressed, leftPressed, rightPressed;

    // Player Ability & Shooting (FIXED COOLDOWN LOGIC)
    private int boostTimer = 0;
    private final int BOOST_DURATION_BASE = 35;
    private int boostCooldownTimer = 0; 
    private final int BASE_BOOST_COOLDOWN_FRAMES = 300; // 5 Seconds at 60 FPS
    private int shootCooldown = 0;

    // Game Variables
    private double scrollSpeed = 3.5;
    private double exactYards = 0;
    private int yardsGained = 0;
    private int totalCoins = 0;
    private boolean gameOver = false;
    private boolean inStore = false;
    private boolean pendingShockwave = false;

    // Boss Tracking
    private MegaBoss activeBoss = null;
    private int nextBossMilestone = 1000;

    // Screen Shake & Juice Effects
    private int screenShakeTimer = 0;
    private int shakeIntensity = 0;

    // Upgrades
    private int speedLevel = 1;
    private int magnetLevel = 0;
    private boolean hasShield = false;

    // Field & Entities
    private double lineScrollX = 0;
    private final List<Defender> defenders = new ArrayList<>();
    private final List<Coin> coins = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<RefereeBoss> referees = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Trail> trails = new ArrayList<>();
    private final List<FootballProjectile> footballs = new ArrayList<>();
    private final List<SonicWave> sonicWaves = new ArrayList<>();
    private final List<BossLaser> bossLasers = new ArrayList<>();
    private final Random random = new Random();
    private int spawnTimer = 0;

    // Safe Removal Queues
    private final List<RefereeBoss> refereesToRemove = new ArrayList<>();

    public pang() {
        this.setFocusable(true);
        this.addKeyListener(this);
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Screen Shake Offset
        int shakeX = 0;
        int shakeY = 0;
        if (screenShakeTimer > 0) {
            shakeX = random.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            shakeY = random.nextInt(shakeIntensity * 2 + 1) - shakeIntensity;
            screenShakeTimer--;
        }
        g2d.translate(shakeX, shakeY);

        int width = getWidth();
        int height = getHeight();

        // 1. Stadium Bleachers
        g2d.setColor(new Color(20, 25, 40));
        g2d.fillRect(0, 0, width, (int) fieldTop);

        g2d.setColor(new Color(60, 70, 90));
        for (int x = 10; x < width; x += 16) {
            for (int y = 15; y < fieldTop - 10; y += 12) {
                g2d.fillRect(x, y, 6, 6);
            }
        }

        // Sidelines
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, (int) fieldTop - 6, width, 6);
        g2d.fillRect(0, (int) fieldBottom, width, 6);

        // 2. Football Turf
        int stripeWidth = 60;
        for (int x = -60; x < width + 60; x += stripeWidth) {
            int renderX = (int) (x - (lineScrollX % stripeWidth));
            if ((x / stripeWidth) % 2 == 0) {
                g2d.setColor(new Color(40, 140, 50));
            } else {
                g2d.setColor(new Color(30, 120, 40));
            }
            g2d.fillRect(renderX, (int) fieldTop, stripeWidth, (int) (fieldBottom - fieldTop));
        }

        // Yard Lines
        g2d.setColor(new Color(255, 255, 255, 180));
        for (int x = 0; x < width + 100; x += 100) {
            int renderX = (int) (x - (lineScrollX % 100));
            g2d.fillRect(renderX, (int) fieldTop, 4, (int) (fieldBottom - fieldTop));
        }

        // Bottom Field Border
        g2d.setColor(new Color(15, 60, 20));
        g2d.fillRect(0, (int) fieldBottom + 6, width, height - (int) fieldBottom);

        // 3. Render Trails
        for (Trail t : trails) {
            g2d.setColor(t.color);
            g2d.fillRect((int) t.x, (int) t.y, 24, 26);
        }

        // 4. Render Coins & Chests
        for (Coin coin : coins) {
            if (coin.isChest) {
                g2d.setColor(new Color(255, 140, 0));
                g2d.fillRect((int) coin.x, (int) coin.y, 32, 24);
                g2d.setColor(Color.YELLOW);
                g2d.drawRect((int) coin.x, (int) coin.y, 32, 24);
                g2d.fillRect((int) coin.x + 12, (int) coin.y + 8, 8, 8);
            } else {
                g2d.setColor(new Color(255, 215, 0));
                g2d.fillOval((int) coin.x, (int) coin.y, 22, 14);
                g2d.setColor(Color.WHITE);
                g2d.fillRect((int) coin.x + 9, (int) coin.y, 3, 14);
            }
        }

        // 5. Render Defenders
        if (activeBoss == null) {
            for (Defender def : defenders) {
                if (def.isBursting) {
                    g2d.setColor(new Color(255, 50, 0, 160));
                    g2d.fillOval((int) def.x - 8, (int) def.y - 8, 40, 42);
                }
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillOval((int) def.x - 2, (int) def.y + 24, 28, 10);

                g2d.setColor(new Color(200, 30, 30));
                g2d.fillRect((int) def.x, (int) def.y, 24, 26);

                g2d.setColor(new Color(240, 240, 240));
                g2d.fillRect((int) def.x - 2, (int) def.y - 4, 18, 14);
                g2d.setColor(Color.BLACK);
                g2d.fillRect((int) def.x + (def.x < playerX ? 14 : -4), (int) def.y, 6, 4);
            }
        }

        // 6. Render Referee Bosses
        if (activeBoss == null) {
            for (RefereeBoss ref : referees) {
                double difficultyMultiplier = 1.0 + (yardsGained / 2500.0);
                int laserHeight = (int) (12 * Math.min(1.5, difficultyMultiplier));

                if (ref.laserCooldown < 25 && ref.laserActiveTimer == 0) {
                    g2d.setColor(new Color(255, 0, 0, 120));
                    g2d.fillRect(0, (int) ref.y + 12, (int) ref.x, 3);
                }

                if (ref.laserActiveTimer > 0) {
                    g2d.setColor(new Color(255, 30, 0, 230));
                    g2d.fillRect(0, (int) ref.y + 14 - (laserHeight / 2), (int) ref.x, laserHeight);
                    g2d.setColor(Color.YELLOW);
                    g2d.fillRect(0, (int) ref.y + 14 - (laserHeight / 4), (int) ref.x, laserHeight / 2);
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, (int) ref.y + 12, (int) ref.x, 4);
                }

                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillOval((int) ref.x - 4, (int) ref.y + 32, 36, 12);

                g2d.setColor(Color.WHITE);
                g2d.fillRect((int) ref.x, (int) ref.y, 28, 32);
                g2d.setColor(Color.BLACK);
                g2d.fillRect((int) ref.x + 6, (int) ref.y, 5, 32);
                g2d.fillRect((int) ref.x + 17, (int) ref.y, 5, 32);

                g2d.setColor(Color.BLACK);
                g2d.fillRect((int) ref.x - 2, (int) ref.y - 6, 32, 8);
            }
        }

        // 7. Render JUICY Mega Boss
        if (activeBoss != null) {
            // Shadow
            g2d.setColor(new Color(0, 0, 0, 120));
            g2d.fillOval((int) activeBoss.x - 10, (int) activeBoss.y + 90, 100, 20);

            // Thruster Glow
            g2d.setColor(new Color(0, 220, 255, 180));
            g2d.fillOval((int) activeBoss.x + 65, (int) activeBoss.y + 20 + random.nextInt(6), 25, 45);

            // Main Body
            if (activeBoss.hitFlashTimer > 0) {
                g2d.setColor(Color.WHITE);
                activeBoss.hitFlashTimer--;
            } else {
                g2d.setColor(new Color(180, 30, 30));
            }
            g2d.fillRect((int) activeBoss.x, (int) activeBoss.y, 75, 90);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawRect((int) activeBoss.x, (int) activeBoss.y, 75, 90);

            // Armored Plates & Details
            g2d.setColor(new Color(100, 20, 20));
            g2d.fillRect((int) activeBoss.x + 10, (int) activeBoss.y + 10, 55, 20);
            g2d.fillRect((int) activeBoss.x + 10, (int) activeBoss.y + 60, 55, 20);

            // Visor Eye Glow
            g2d.setColor(Color.CYAN);
            g2d.fillRect((int) activeBoss.x - 15, (int) activeBoss.y + 30, 30, 25);
            g2d.setColor(Color.WHITE);
            g2d.drawRect((int) activeBoss.x - 15, (int) activeBoss.y + 30, 30, 25);
            g2d.fillRect((int) activeBoss.x - 10, (int) activeBoss.y + 38, 12, 8);

            // Boss Health Bar UI
            g2d.setColor(new Color(20, 20, 30, 200));
            g2d.fillRect(width / 2 - 160, 15, 320, 30);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(width / 2 - 160, 15, 320, 30);

            double hpRatio = Math.max(0, (double) activeBoss.hp / activeBoss.maxHp);
            int currentHpWidth = (int) (312 * hpRatio);

            g2d.setColor(hpRatio > 0.4 ? Color.RED : Color.ORANGE);
            g2d.fillRect(width / 2 - 156, 19, currentHpWidth, 22);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2d.drawString("MECH COACH: " + activeBoss.hp + " / " + activeBoss.maxHp, width / 2 - 90, 35);
        }

        // 8. Render Projectiles
        for (FootballProjectile fb : footballs) {
            g2d.setColor(new Color(139, 69, 19));
            g2d.fillOval((int) fb.x, (int) fb.y, 16, 10);
            g2d.setColor(Color.WHITE);
            g2d.fillRect((int) fb.x + 6, (int) fb.y, 4, 10);
        }

        for (SonicWave sw : sonicWaves) {
            g2d.setColor(new Color(255, 50, 200, 180));
            g2d.drawOval((int) sw.x - sw.radius, (int) sw.y - sw.radius, sw.radius * 2, sw.radius * 2);
            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.drawOval((int) sw.x - sw.radius + 2, (int) sw.y - sw.radius + 2, (sw.radius - 2) * 2, (sw.radius - 2) * 2);
        }

        for (BossLaser bl : bossLasers) {
            g2d.setColor(Color.CYAN);
            g2d.fillRect((int) bl.x, (int) bl.y, 22, 8);
            g2d.setColor(Color.WHITE);
            g2d.fillRect((int) bl.x + 2, (int) bl.y + 2, 18, 4);
        }

        // 9. Render Particles
        for (Particle p : particles) {
            g2d.setColor(p.color);
            g2d.fillRect((int) p.x, (int) p.y, p.size, p.size);
        }

        // 10. Render Player
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillOval((int) playerX - 2, (int) playerY + 24, 28, 10);

        if (boostTimer > 0) {
            g2d.setColor(new Color(255, 215, 0, 180));
            g2d.fillOval((int) playerX - 12, (int) playerY - 12, 48, 50);
            g2d.setColor(new Color(255, 60, 0, 120));
            g2d.fillOval((int) playerX - 18, (int) playerY - 18, 60, 62);
        } else if (hasShield) {
            g2d.setColor(new Color(0, 191, 255, 140));
            g2d.fillOval((int) playerX - 8, (int) playerY - 8, 40, 42);
        }

        g2d.setColor(new Color(30, 90, 220));
        g2d.fillRect((int) playerX, (int) playerY, 24, 26);

        g2d.setColor(new Color(255, 215, 0));
        g2d.fillRect((int) playerX + 8, (int) playerY - 4, 18, 14);
        g2d.setColor(Color.BLACK);
        g2d.fillRect((int) playerX + 22, (int) playerY, 6, 4);

        g2d.setColor(new Color(139, 69, 19));
        g2d.fillOval((int) playerX + 10, (int) playerY + 12, 12, 8);

        // 11. Render Pop-Up Text
        for (FloatingText ft : floatingTexts) {
            g2d.setColor(ft.color);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2d.drawString(ft.text, (int) ft.x, (int) ft.y);
        }

        // 12. HUD (Refined Boost Indicator)
        g2d.setColor(new Color(10, 15, 25));
        g2d.fillRect(15, 10, 290, 100);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(15, 10, 290, 100);

        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2d.drawString("YARDS: " + yardsGained, 25, 30);

        g2d.setColor(Color.YELLOW);
        g2d.drawString("COINS: " + totalCoins, 25, 50);

        int maxCooldown = Math.max(120, BASE_BOOST_COOLDOWN_FRAMES - (speedLevel * 30));
        if (boostCooldownTimer <= 0) {
            g2d.setColor(Color.GREEN);
            g2d.drawString("BOOST [SPACE]: READY", 25, 70);
        } else {
            g2d.setColor(Color.ORANGE);
            double secondsLeft = (double) boostCooldownTimer / 60.0;
            g2d.drawString(String.format("BOOST: %.1fs", secondsLeft), 25, 70);

            // Cooldown Mini Progress Bar
            g2d.setColor(Color.GRAY);
            g2d.fillRect(175, 58, 110, 12);
            g2d.setColor(Color.YELLOW);
            double fillRatio = 1.0 - ((double) boostCooldownTimer / maxCooldown);
            g2d.fillRect(175, 58, (int) (110 * fillRatio), 12);
            g2d.setColor(Color.WHITE);
            g2d.drawRect(175, 58, 110, 12);
        }

        g2d.setColor(Color.ORANGE);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2d.drawString("THROW FOOTBALL: [F] or [K]", 25, 92);

        // Overlays
        if (inStore) {
            renderStore(g2d, width, height);
        } else if (gameOver) {
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRect(0, 0, width, height);

            g2d.setColor(new Color(255, 60, 60));
            g2d.setFont(new Font("Monospaced", Font.BOLD, 42));
            g2d.drawString("TACKLED!", width / 2 - 110, height / 2 - 40);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 18));
            g2d.drawString("Press R to Run Again", width / 2 - 110, height / 2 + 10);

            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
            g2d.drawString("Press S for Store", width / 2 - 110, height / 2 + 50);
        }
    }

    private void renderStore(Graphics2D g2d, int width, int height) {
        g2d.setColor(new Color(15, 20, 30, 240));
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 32));
        g2d.drawString("LOCKER ROOM STORE", width / 2 - 160, 55);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2d.drawString("Banked Coins: " + totalCoins, width / 2 - 80, 85);

        int boxY = 115;
        drawStoreItem(g2d, 80, boxY, width - 160, 80,
            "[1] Overdrive Boots (Lvl " + speedLevel + ")",
            "Speed +" + (speedLevel * 12) + "% | Boost Cooldown -" + (speedLevel * 8) + "%",
            "Cost: " + (speedLevel * 25) + " Coins");

        drawStoreItem(g2d, 80, boxY + 95, width - 160, 80,
            "[2] Quantum Magnet (Lvl " + magnetLevel + ")",
            magnetLevel == 0 ? "Pulls distant coins instantly" : "Magnet Range +" + (magnetLevel * 60) + "px",
            "Cost: " + ((magnetLevel + 1) * 30) + " Coins");

        drawStoreItem(g2d, 80, boxY + 190, width - 160, 80,
            "[3] Sonic Shockwave Shield",
            "Absorbs 1 hit & triggers full-screen blast",
            hasShield ? "OWNED" : "Cost: 50 Coins");

        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        g2d.drawString("Press ESC to Exit Store", width / 2 - 120, height - 35);
    }

    private void drawStoreItem(Graphics2D g2d, int x, int y, int w, int h, String title, String desc, String cost) {
        g2d.setColor(new Color(35, 45, 65));
        g2d.fillRect(x, y, w, h);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, w, h);

        g2d.setColor(Color.YELLOW);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
        g2d.drawString(title, x + 20, y + 26);

        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2d.drawString(desc, x + 20, y + 50);

        g2d.setColor(Color.GREEN);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 15));
        g2d.drawString(cost, x + w - 190, y + 42);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver || inStore) return;

        double difficultyFactor = 1.0 + (yardsGained / 2500.0);
        double currentScrollSpeed = scrollSpeed + (yardsGained * 0.0003);
        double activeMoveSpeed = baseMoveSpeed + (speedLevel * 0.7);

        // Boost Ability Execution
        if (boostTimer > 0) {
            activeMoveSpeed *= 1.85;
            boostTimer--;
            trails.add(new Trail(playerX, playerY, new Color(255, 140, 0, 150)));
        } else if (random.nextDouble() < 0.3) {
            trails.add(new Trail(playerX, playerY, new Color(30, 90, 220, 80)));
        }

        // Cooldown Decrement
        if (boostCooldownTimer > 0) boostCooldownTimer--;
        if (shootCooldown > 0) shootCooldown--;

        double playerVx = 0;
        double playerVy = 0;

        if (upPressed && playerY > fieldTop + 5) playerVy -= activeMoveSpeed;
        if (downPressed && playerY < fieldBottom - 30) playerVy += activeMoveSpeed;
        if (rightPressed && playerX < getWidth() - 50) playerVx += activeMoveSpeed;

        if (leftPressed && playerX > 20) {
            playerVx -= (activeMoveSpeed + currentScrollSpeed);
        } else {
            playerVx += currentScrollSpeed;
        }

        playerX += (playerVx - currentScrollSpeed);
        playerY += playerVy;

        if (playerX < 20) playerX = 20;
        if (playerX > getWidth() - 50) playerX = getWidth() - 50;

        lineScrollX += currentScrollSpeed;

        exactYards += (currentScrollSpeed * 0.05);
        yardsGained = (int) exactYards;

        // Boss Milestone Spawn
        if (yardsGained >= nextBossMilestone && activeBoss == null) {
            activeBoss = new MegaBoss(getWidth() + 80, 200, 25 + (yardsGained / 150));
            nextBossMilestone += 1000;
            defenders.clear();
            referees.clear();
            floatingTexts.add(new FloatingText(getWidth() / 2 - 110, 150, "WARNING: MECH COACH!", Color.RED));
            triggerScreenShake(12, 25);
        }

        // Spawn Field Enemies
        if (activeBoss == null) {
            spawnTimer++;
            int dynamicSpawnDelay = Math.max(45, (int) (120 / difficultyFactor));
            if (spawnTimer > dynamicSpawnDelay) {
                int spawnY = (int) (fieldTop + random.nextInt((int) (fieldBottom - fieldTop - 30)));
                if (random.nextDouble() < 0.75) {
                    double runSpeed = (2.0 + random.nextDouble() * 1.2) * difficultyFactor;
                    int reactionDelay = Math.max(5, 20 - (yardsGained / 250));
                    defenders.add(new Defender(getWidth() + 20, spawnY, runSpeed, reactionDelay));
                } else {
                    coins.add(new Coin(getWidth() + 20, spawnY, 3, false));
                }
                spawnTimer = 0;
            }
        }

        // Football Projectiles Logic
        Iterator<FootballProjectile> fbIter = footballs.iterator();
        while (fbIter.hasNext()) {
            FootballProjectile fb = fbIter.next();
            fb.x += 12.0;

            if (activeBoss != null) {
                if (fb.x > activeBoss.x - 20 && fb.x < activeBoss.x + 75 && fb.y > activeBoss.y && fb.y < activeBoss.y + 90) {
                    activeBoss.hp -= 2;
                    activeBoss.hitFlashTimer = 4;
                    spawnParticles((int) fb.x, (int) fb.y, Color.ORANGE, 10);
                    floatingTexts.add(new FloatingText(fb.x, fb.y, "-2 HP", Color.YELLOW));
                    triggerScreenShake(3, 5);
                    fbIter.remove();
                    continue;
                }
            }

            if (fb.x > getWidth() + 20) fbIter.remove();
        }

        // Mega Boss Juiced Logic
        if (activeBoss != null) {
            if (activeBoss.x > getWidth() - 140) activeBoss.x -= 2.0;

            activeBoss.y += activeBoss.vy;
            if (activeBoss.y < fieldTop + 10 || activeBoss.y > fieldBottom - 90) {
                activeBoss.vy *= -1;
            }

            activeBoss.attackTimer--;
            if (activeBoss.attackTimer <= 0) {
                if (random.nextBoolean()) {
                    sonicWaves.add(new SonicWave(activeBoss.x - 10, activeBoss.y + 40));
                } else {
                    bossLasers.add(new BossLaser(activeBoss.x - 10, activeBoss.y + 20));
                    bossLasers.add(new BossLaser(activeBoss.x - 10, activeBoss.y + 60));
                }
                activeBoss.attackTimer = Math.max(35, 55 - (yardsGained / 500));
            }

            if (activeBoss.hp <= 0) {
                triggerScreenShake(20, 35);
                spawnParticles((int) activeBoss.x + 35, (int) activeBoss.y + 45, Color.YELLOW, 60);
                spawnParticles((int) activeBoss.x + 35, (int) activeBoss.y + 45, Color.ORANGE, 40);
                floatingTexts.add(new FloatingText(activeBoss.x, activeBoss.y - 20, "BOSS DESTROYED!", Color.GREEN));

                // Drop Giant Reward Chest
                coins.add(new Coin(activeBoss.x, activeBoss.y, 75, true));
                activeBoss = null;
            }
        }

        // Boss Lasers Update
        Iterator<BossLaser> blIter = bossLasers.iterator();
        while (blIter.hasNext()) {
            BossLaser bl = blIter.next();
            bl.x -= 8.5;

            if (Math.abs(bl.x - playerX) < 20 && Math.abs(bl.y - playerY) < 20) {
                if (boostTimer <= 0) {
                    if (hasShield) {
                        hasShield = false;
                        triggerSonicShockwave();
                    } else {
                        gameOver = true;
                    }
                }
                blIter.remove();
                continue;
            }

            if (bl.x < -30) blIter.remove();
        }

        // Sonic Waves Update
        Iterator<SonicWave> swIter = sonicWaves.iterator();
        while (swIter.hasNext()) {
            SonicWave sw = swIter.next();
            sw.x -= 5.0;
            sw.radius += 1;

            if (Math.abs(sw.x - playerX) < sw.radius && Math.abs(sw.y - playerY) < sw.radius) {
                if (boostTimer <= 0) {
                    if (hasShield) {
                        hasShield = false;
                        triggerSonicShockwave();
                    } else {
                        gameOver = true;
                    }
                }
            }

            if (sw.x < -40) swIter.remove();
        }

        refereesToRemove.clear();

        // Defender Logic
        if (activeBoss == null) {
            Iterator<Defender> defIter = defenders.iterator();
            while (defIter.hasNext()) {
                Defender def = defIter.next();
                def.x -= currentScrollSpeed;

                boolean killedByLaser = false;
                for (RefereeBoss ref : referees) {
                    if (ref.laserActiveTimer > 0) {
                        double laserTolerance = (12 * Math.min(1.5, difficultyFactor)) / 2.0;

                        if (def.x < ref.x && Math.abs((def.y + 13) - (ref.y + 14)) < laserTolerance + 10) {
                            spawnParticles((int) def.x, (int) def.y, Color.RED, 15);
                            floatingTexts.add(new FloatingText(def.x, def.y - 10, "VAPORIZED!", Color.RED));
                            dropDefenderCoins(def.x, def.y);
                            defIter.remove();
                            killedByLaser = true;
                            break;
                        }
                    }
                }

                if (killedByLaser) continue;

                if (def.reactionTimer > 0) {
                    def.reactionTimer--;
                } else {
                    double dx = playerX - def.x;
                    double dy = playerY - def.y;
                    double distance = Math.hypot(dx, dy);

                    if (distance > 0) {
                        def.x += (dx / distance) * def.speed;
                        def.y += (dy / distance) * def.speed;
                    }
                }

                // Player Collision
                if (Math.abs(def.x - playerX) < 22 && Math.abs(def.y - playerY) < 22) {
                    if (boostTimer > 0) {
                        triggerScreenShake(8, 12);
                        spawnParticles((int) def.x, (int) def.y, Color.ORANGE, 16);
                        floatingTexts.add(new FloatingText(def.x, def.y - 10, "SMASH!", Color.ORANGE));
                        dropDefenderCoins(def.x, def.y);
                        referees.add(new RefereeBoss(680, def.y));
                        defIter.remove();
                        continue;
                    } else if (hasShield) {
                        hasShield = false;
                        triggerSonicShockwave();
                        defIter.remove();
                        continue;
                    } else {
                        gameOver = true;
                    }
                }

                if (def.x < -60 || def.x > getWidth() + 150) defIter.remove();
            }
        }

        // Referee Logic
        if (activeBoss == null) {
            for (RefereeBoss ref : referees) {
                ref.lifeTimer--;
                if (ref.x < 680) ref.x += 4.0;

                ref.laserCooldown--;
                int activeLaserThreshold = Math.max(15, (int) (25 / difficultyFactor));
                int laserCooldownMax = Math.max(60, (int) (120 / difficultyFactor));

                if (ref.laserCooldown <= 0) {
                    ref.laserActiveTimer = activeLaserThreshold;
                    ref.laserCooldown = laserCooldownMax;
                }

                if (ref.laserActiveTimer > 0) {
                    ref.laserActiveTimer--;
                    double laserTolerance = 12 * Math.min(1.5, difficultyFactor);
                    if (Math.abs(playerY - ref.y) < laserTolerance && playerX < ref.x) {
                        if (boostTimer <= 0) {
                            if (hasShield) {
                                hasShield = false;
                                triggerSonicShockwave();
                            } else {
                                gameOver = true;
                            }
                        }
                    }
                }

                if (ref.lifeTimer <= 0) {
                    spawnParticles((int) ref.x, (int) ref.y, Color.YELLOW, 20);
                    floatingTexts.add(new FloatingText(ref.x, ref.y - 10, "+15 CHEST!", Color.YELLOW));
                    coins.add(new Coin(ref.x, ref.y, 15, true));
                    refereesToRemove.add(ref);
                }
            }
            referees.removeAll(refereesToRemove);
        }

        // Coins & Magnet Update
        Iterator<Coin> coinIter = coins.iterator();
        while (coinIter.hasNext()) {
            Coin coin = coinIter.next();
            coin.x -= currentScrollSpeed;

            if (magnetLevel > 0) {
                double magnetRange = 120 + (magnetLevel * 60);
                double dx = playerX - coin.x;
                double dy = playerY - coin.y;
                double dist = Math.hypot(dx, dy);

                if (dist < magnetRange && dist > 0) {
                    double magnetPullSpeed = 9.0 + (magnetLevel * 2.0);
                    coin.x += (dx / dist) * magnetPullSpeed;
                    coin.y += (dy / dist) * magnetPullSpeed;
                }
            }

            if (Math.abs(coin.x - playerX) < 24 && Math.abs(coin.y - playerY) < 24) {
                totalCoins += coin.value;
                spawnParticles((int) playerX, (int) playerY, Color.YELLOW, coin.isChest ? 16 : 6);
                floatingTexts.add(new FloatingText(playerX, playerY - 10, "+" + coin.value, Color.YELLOW));
                coinIter.remove();
                continue;
            }

            if (coin.x < -30) coinIter.remove();
        }

        // Particles Update
        Iterator<Particle> pIter = particles.iterator();
        while (pIter.hasNext()) {
            Particle p = pIter.next();
            p.x += p.vx;
            p.y += p.vy;
            p.life--;
            if (p.life <= 0) pIter.remove();
        }

        // Trails Update
        Iterator<Trail> tIter = trails.iterator();
        while (tIter.hasNext()) {
            Trail t = tIter.next();
            t.life--;
            if (t.life <= 0) tIter.remove();
        }

        // Floating Text Update
        Iterator<FloatingText> ftIter = floatingTexts.iterator();
        while (ftIter.hasNext()) {
            FloatingText ft = ftIter.next();
            ft.y -= 1.2;
            ft.life--;
            if (ft.life <= 0) ftIter.remove();
        }

        // Deferred Shockwave Handling
        if (pendingShockwave) {
            triggerScreenShake(12, 18);
            spawnParticles((int) playerX, (int) playerY, Color.CYAN, 40);
            floatingTexts.add(new FloatingText(playerX, playerY - 15, "SONIC BLAST!", Color.CYAN));

            for (Defender d : defenders) dropDefenderCoins(d.x, d.y);
            defenders.clear();
            referees.clear();
            bossLasers.clear();
            pendingShockwave = false;
        }

        repaint();
    }

    private void triggerScreenShake(int intensity, int duration) {
        this.shakeIntensity = intensity;
        this.screenShakeTimer = duration;
    }

    private void dropDefenderCoins(double x, double y) {
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 30;
            double offsetY = (random.nextDouble() - 0.5) * 30;
            coins.add(new Coin(x + offsetX, y + offsetY, 3, false));
        }
    }

    private void triggerSonicShockwave() {
        pendingShockwave = true;
    }

    private void spawnParticles(int x, int y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                x, y,
                (random.nextDouble() - 0.5) * 10,
                (random.nextDouble() - 0.5) * 10,
                color,
                random.nextInt(5) + 3
            ));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (inStore) {
            handleStoreInput(key);
            return;
        }

        if (!gameOver) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) upPressed = true;
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) downPressed = true;
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) leftPressed = true;
            if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) rightPressed = true;

            // Space Boost Trigger with Fixed Cooldown Calculation
            if (key == KeyEvent.VK_SPACE && boostCooldownTimer <= 0) {
                boostTimer = BOOST_DURATION_BASE + (speedLevel * 4);
                int reducedCooldown = BASE_BOOST_COOLDOWN_FRAMES - (speedLevel * 30);
                boostCooldownTimer = Math.max(120, reducedCooldown);
                triggerScreenShake(6, 10);
            }

            if ((key == KeyEvent.VK_F || key == KeyEvent.VK_K) && shootCooldown <= 0) {
                footballs.add(new FootballProjectile(playerX + 24, playerY + 8));
                shootCooldown = 15;
            }
        }

        if (key == KeyEvent.VK_R && gameOver) {
            resetGame();
        }

        if (key == KeyEvent.VK_S && gameOver) {
            inStore = true;
            repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) upPressed = false;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) downPressed = false;
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) leftPressed = false;
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) rightPressed = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    private void handleStoreInput(int key) {
        if (key == KeyEvent.VK_ESCAPE) {
            inStore = false;
        } else if (key == KeyEvent.VK_1) {
            int cost = speedLevel * 25;
            if (totalCoins >= cost) {
                totalCoins -= cost;
                speedLevel++;
            }
        } else if (key == KeyEvent.VK_2) {
            int cost = (magnetLevel + 1) * 30;
            if (totalCoins >= cost) {
                totalCoins -= cost;
                magnetLevel++;
            }
        } else if (key == KeyEvent.VK_3) {
            int cost = 50;
            if (!hasShield && totalCoins >= cost) {
                totalCoins -= cost;
                hasShield = true;
            }
        }
        repaint();
    }

    private void resetGame() {
        playerY = 250;
        playerX = 120;
        exactYards = 0;
        yardsGained = 0;
        boostTimer = 0;
        boostCooldownTimer = 0;
        shootCooldown = 0;
        gameOver = false;
        inStore = false;
        activeBoss = null;
        nextBossMilestone = 1000;
        defenders.clear();
        coins.clear();
        particles.clear();
        referees.clear();
        floatingTexts.clear();
        trails.clear();
        footballs.clear();
        sonicWaves.clear();
        bossLasers.clear();
        repaint();
    }

    // Entity Data Classes
    private static class Defender {
        double x, y, speed;
        int reactionTimer, burstTimer;
        boolean isBursting = false;

        Defender(double x, double y, double speed, int reactionTimer) {
            this.x = x; this.y = y; this.speed = speed; this.reactionTimer = reactionTimer;
        }
    }

    private static class Coin {
        double x, y;
        int value;
        boolean isChest;

        Coin(double x, double y, int value, boolean isChest) {
            this.x = x; this.y = y; this.value = value; this.isChest = isChest;
        }
    }

    private static class Particle {
        double x, y, vx, vy;
        Color color;
        int life, size;

        Particle(double x, double y, double vx, double vy, Color color, int life) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color;
            this.life = life; this.size = life;
        }
    }

    private static class RefereeBoss {
        double x, y;
        int lifeTimer = 600;
        int laserCooldown = 60;
        int laserActiveTimer = 0;

        RefereeBoss(double x, double y) { this.x = x; this.y = y; }
    }

    private static class MegaBoss {
        double x, y, vy = 2.0;
        int hp, maxHp;
        int attackTimer = 50;
        int hitFlashTimer = 0;

        MegaBoss(double x, double y, int hp) {
            this.x = x; this.y = y; this.hp = hp; this.maxHp = hp;
        }
    }

    private static class FootballProjectile {
        double x, y;
        FootballProjectile(double x, double y) { this.x = x; this.y = y; }
    }

    private static class SonicWave {
        double x, y;
        int radius = 10;
        SonicWave(double x, double y) { this.x = x; this.y = y; }
    }

    private static class BossLaser {
        double x, y;
        BossLaser(double x, double y) { this.x = x; this.y = y; }
    }

    private static class FloatingText {
        double x, y;
        String text;
        Color color;
        int life = 35;

        FloatingText(double x, double y, String text, Color color) {
            this.x = x; this.y = y; this.text = text; this.color = color;
        }
    }

    private static class Trail {
        double x, y;
        Color color;
        int life = 8;

        Trail(double x, double y, Color color) {
            this.x = x; this.y = y; this.color = color;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Retro Football Runner 2D");
        pang gamePanel = new pang();
        frame.add(gamePanel);
        frame.setSize(800, 520);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }
}