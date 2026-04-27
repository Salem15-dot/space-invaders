import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GameModel {
    public static final int WORLD_WIDTH = 800;
    public static final int WORLD_HEIGHT = 600;

    public static final int PLAYER_WIDTH = 40;
    public static final int PLAYER_HEIGHT = 20;
    public static final int PLAYER_SPEED = 12;
    public static final int PLAYER_Y = WORLD_HEIGHT - 60;

    public static final int ALIEN_ROWS = 5;
    public static final int ALIEN_COLS = 11;
    public static final int ALIEN_WIDTH = 32;
    public static final int ALIEN_HEIGHT = 20;
    public static final int ALIEN_H_SPACING = 14;
    public static final int ALIEN_V_SPACING = 14;
    public static final int ALIEN_START_X = 80;
    public static final int ALIEN_START_Y = 70;
    public static final int ALIEN_SPEED_X = 8;
    public static final int ALIEN_STEP_DOWN = 20;

    public static final int PLAYER_BULLET_WIDTH = 4;
    public static final int PLAYER_BULLET_HEIGHT = 12;
    public static final int PLAYER_BULLET_SPEED = 14;

    public static final int ALIEN_BULLET_WIDTH = 4;
    public static final int ALIEN_BULLET_HEIGHT = 10;
    public static final int ALIEN_BULLET_SPEED = 8;

    private static final int STARTING_LIVES = 3;
    private static final int ALIEN_FIRE_MIN_COOLDOWN = 15;
    private static final int ALIEN_FIRE_MAX_COOLDOWN = 55;
    private static final int SCORE_PER_ALIEN = 10;

    private final boolean[][] aliensAlive = new boolean[ALIEN_ROWS][ALIEN_COLS];
    private final List<Bullet> alienBullets = new ArrayList<>();
    private final Random random = new Random();

    private int playerX;
    private int aliensOriginX;
    private int aliensOriginY;
    private int aliensDirectionX = 1;
    private Bullet playerBullet;
    private int alienFireCooldownTicks;
    private int score;
    private int lives;

    public GameModel() {
        playerX = (WORLD_WIDTH - PLAYER_WIDTH) / 2;
        aliensOriginX = ALIEN_START_X;
        aliensOriginY = ALIEN_START_Y;
        lives = STARTING_LIVES;

        for (int row = 0; row < ALIEN_ROWS; row++) {
            for (int col = 0; col < ALIEN_COLS; col++) {
                aliensAlive[row][col] = true;
            }
        }

        resetAlienFireCooldown();
    }

    public void movePlayerLeft() {
        playerX = Math.max(0, playerX - PLAYER_SPEED);
    }

    public void movePlayerRight() {
        playerX = Math.min(WORLD_WIDTH - PLAYER_WIDTH, playerX + PLAYER_SPEED);
    }

    public void firePlayerBullet() {
        if (playerBullet != null || lives <= 0) {
            return;
        }

        int bulletX = playerX + (PLAYER_WIDTH - PLAYER_BULLET_WIDTH) / 2;
        int bulletY = PLAYER_Y - PLAYER_BULLET_HEIGHT;
        playerBullet = new Bullet(bulletX, bulletY, PLAYER_BULLET_WIDTH, PLAYER_BULLET_HEIGHT, -PLAYER_BULLET_SPEED);
    }

    public void tick() {
        if (lives <= 0) {
            return;
        }

        moveAliens();
        advancePlayerBullet();
        advanceAlienBullets();
        fireAlienBulletIfReady();
        detectCollisions();
    }

    public int getPlayerX() {
        return playerX;
    }

    public int getPlayerY() {
        return PLAYER_Y;
    }

    public int getAliensOriginX() {
        return aliensOriginX;
    }

    public int getAliensOriginY() {
        return aliensOriginY;
    }

    public boolean isAlienAlive(int row, int col) {
        return aliensAlive[row][col];
    }

    public boolean[][] copyAliensAlive() {
        boolean[][] copy = new boolean[ALIEN_ROWS][ALIEN_COLS];
        for (int row = 0; row < ALIEN_ROWS; row++) {
            System.arraycopy(aliensAlive[row], 0, copy[row], 0, ALIEN_COLS);
        }
        return copy;
    }

    public Bullet getPlayerBullet() {
        return playerBullet;
    }

    public List<Bullet> getAlienBullets() {
        return Collections.unmodifiableList(alienBullets);
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public boolean isGameOver() {
        return lives <= 0;
    }

    private void moveAliens() {
        if (!anyAliensAlive()) {
            return;
        }

        int leftmost = Integer.MAX_VALUE;
        int rightmost = Integer.MIN_VALUE;

        for (int row = 0; row < ALIEN_ROWS; row++) {
            for (int col = 0; col < ALIEN_COLS; col++) {
                if (!aliensAlive[row][col]) {
                    continue;
                }

                int alienX = aliensOriginX + col * (ALIEN_WIDTH + ALIEN_H_SPACING);
                leftmost = Math.min(leftmost, alienX);
                rightmost = Math.max(rightmost, alienX + ALIEN_WIDTH);
            }
        }

        int proposedOffset = aliensDirectionX * ALIEN_SPEED_X;
        if (leftmost + proposedOffset < 0 || rightmost + proposedOffset > WORLD_WIDTH) {
            aliensDirectionX *= -1;
            aliensOriginY += ALIEN_STEP_DOWN;
        } else {
            aliensOriginX += proposedOffset;
        }
    }

    private void advancePlayerBullet() {
        if (playerBullet == null) {
            return;
        }

        playerBullet.y += playerBullet.vy;
        if (playerBullet.y + playerBullet.height < 0) {
            playerBullet = null;
        }
    }

    private void advanceAlienBullets() {
        for (int i = alienBullets.size() - 1; i >= 0; i--) {
            Bullet bullet = alienBullets.get(i);
            bullet.y += bullet.vy;
            if (bullet.y > WORLD_HEIGHT) {
                alienBullets.remove(i);
            }
        }
    }

    private void fireAlienBulletIfReady() {
        if (!anyAliensAlive()) {
            return;
        }

        alienFireCooldownTicks--;
        if (alienFireCooldownTicks > 0) {
            return;
        }

        int[] firingAlien = pickRandomBottomAliveAlien();
        if (firingAlien != null) {
            int row = firingAlien[0];
            int col = firingAlien[1];
            int alienX = aliensOriginX + col * (ALIEN_WIDTH + ALIEN_H_SPACING);
            int alienY = aliensOriginY + row * (ALIEN_HEIGHT + ALIEN_V_SPACING);
            int bulletX = alienX + (ALIEN_WIDTH - ALIEN_BULLET_WIDTH) / 2;
            int bulletY = alienY + ALIEN_HEIGHT;

            alienBullets.add(new Bullet(
                    bulletX,
                    bulletY,
                    ALIEN_BULLET_WIDTH,
                    ALIEN_BULLET_HEIGHT,
                    ALIEN_BULLET_SPEED
            ));
        }

        resetAlienFireCooldown();
    }

    private void detectCollisions() {
        handlePlayerBulletAlienCollision();
        handleAlienBulletPlayerCollisions();
    }

    private void handlePlayerBulletAlienCollision() {
        if (playerBullet == null) {
            return;
        }

        for (int row = 0; row < ALIEN_ROWS; row++) {
            for (int col = 0; col < ALIEN_COLS; col++) {
                if (!aliensAlive[row][col]) {
                    continue;
                }

                int alienX = aliensOriginX + col * (ALIEN_WIDTH + ALIEN_H_SPACING);
                int alienY = aliensOriginY + row * (ALIEN_HEIGHT + ALIEN_V_SPACING);
                if (intersects(playerBullet, alienX, alienY, ALIEN_WIDTH, ALIEN_HEIGHT)) {
                    aliensAlive[row][col] = false;
                    playerBullet = null;
                    score += SCORE_PER_ALIEN;
                    return;
                }
            }
        }
    }

    private void handleAlienBulletPlayerCollisions() {
        for (int i = alienBullets.size() - 1; i >= 0; i--) {
            Bullet bullet = alienBullets.get(i);
            if (intersects(bullet, playerX, PLAYER_Y, PLAYER_WIDTH, PLAYER_HEIGHT)) {
                alienBullets.remove(i);
                lives--;
                if (lives < 0) {
                    lives = 0;
                }

                // Reset player shot on hit to keep state transitions simple.
                playerBullet = null;
                if (lives <= 0) {
                    alienBullets.clear();
                }
                return;
            }
        }
    }

    private boolean anyAliensAlive() {
        for (int row = 0; row < ALIEN_ROWS; row++) {
            for (int col = 0; col < ALIEN_COLS; col++) {
                if (aliensAlive[row][col]) {
                    return true;
                }
            }
        }
        return false;
    }

    private int[] pickRandomBottomAliveAlien() {
        List<int[]> candidates = new ArrayList<>();
        for (int col = 0; col < ALIEN_COLS; col++) {
            for (int row = ALIEN_ROWS - 1; row >= 0; row--) {
                if (aliensAlive[row][col]) {
                    candidates.add(new int[]{row, col});
                    break;
                }
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        return candidates.get(random.nextInt(candidates.size()));
    }

    private void resetAlienFireCooldown() {
        alienFireCooldownTicks = ALIEN_FIRE_MIN_COOLDOWN
                + random.nextInt(ALIEN_FIRE_MAX_COOLDOWN - ALIEN_FIRE_MIN_COOLDOWN + 1);
    }

    private boolean intersects(Bullet bullet, int x, int y, int width, int height) {
        int bulletRight = bullet.x + bullet.width;
        int bulletBottom = bullet.y + bullet.height;
        int targetRight = x + width;
        int targetBottom = y + height;

        return bullet.x < targetRight
                && bulletRight > x
                && bullet.y < targetBottom
                && bulletBottom > y;
    }

    public static class Bullet {
        private int x;
        private int y;
        private final int width;
        private final int height;
        private final int vy;

        public Bullet(int x, int y, int width, int height, int vy) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.vy = vy;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getVy() {
            return vy;
        }
    }
}
