import java.awt.Rectangle;
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

    public static final int UFO_WIDTH = 48;
    public static final int UFO_HEIGHT = 22;
    public static final int UFO_Y = 18;
    public static final int UFO_SPEED_X = 10;

    public static final long ANIM_FRAME_TOGGLE_MS = 2000L;

    public static final int SHIELD_COUNT = 4;
    public static final int SHIELD_WIDTH = 90;
    public static final int SHIELD_HEIGHT = 30;
    public static final int SHIELD_GAP = 60;
    public static final int SHIELD_Y = PLAYER_Y - 120;

    private static final int BASE_TIMER_INTERVAL_MS = 16;
    private static final int MIN_TIMER_INTERVAL_MS = 6;
    private static final int TIMER_INTERVAL_STEP_MS = 1;

    private static final int STARTING_LIVES = 3;
    private static final int ALIEN_FIRE_MIN_COOLDOWN = 15;
    private static final int ALIEN_FIRE_MAX_COOLDOWN = 55;
    private static final int SCORE_PER_ALIEN = 10;
    private static final int SCORE_PER_UFO = 300;
    private static final int UFO_SPAWN_MIN_COOLDOWN = 240;
    private static final int UFO_SPAWN_MAX_COOLDOWN = 480;
    private static final int SHIELD_START_X = (WORLD_WIDTH - (SHIELD_COUNT * SHIELD_WIDTH
            + (SHIELD_COUNT - 1) * SHIELD_GAP)) / 2;

    private final boolean[][] aliensAlive = new boolean[ALIEN_ROWS][ALIEN_COLS];
    private final List<Shield> shields = new ArrayList<>();
    private final List<Bullet> alienBullets = new ArrayList<>();
    private final Random random = new Random();

    private int playerX;
    private int aliensOriginX;
    private int aliensOriginY;
    private int aliensDirectionX = 1;
    private Bullet playerBullet;
    private int alienFireCooldownTicks;
    private int ufoSpawnCooldownTicks;
    private Rectangle ufo;
    private int ufoDirectionX;
    private boolean animFrame;
    private long lastAnimToggleTimeMs;
    private int destroyedAliens;
    private int recommendedTimerIntervalMs = BASE_TIMER_INTERVAL_MS;
    private int score;
    private int lives;

    public GameModel() {
        reset();
    }

    public void reset() {
        playerX = (WORLD_WIDTH - PLAYER_WIDTH) / 2;
        aliensOriginX = ALIEN_START_X;
        aliensOriginY = ALIEN_START_Y;
        aliensDirectionX = 1;
        playerBullet = null;
        alienBullets.clear();
        alienFireCooldownTicks = 0;
        ufoSpawnCooldownTicks = random.nextInt(UFO_SPAWN_MAX_COOLDOWN - UFO_SPAWN_MIN_COOLDOWN + 1)
            + UFO_SPAWN_MIN_COOLDOWN;
        ufo = null;
        ufoDirectionX = 1;
        animFrame = true;
        lastAnimToggleTimeMs = System.currentTimeMillis();
        destroyedAliens = 0;
        recommendedTimerIntervalMs = BASE_TIMER_INTERVAL_MS;
        score = 0;
        lives = STARTING_LIVES;

        for (int row = 0; row < ALIEN_ROWS; row++) {
            for (int col = 0; col < ALIEN_COLS; col++) {
                aliensAlive[row][col] = true;
            }
        }

        shields.clear();
        for (int index = 0; index < SHIELD_COUNT; index++) {
            int shieldX = SHIELD_START_X + index * (SHIELD_WIDTH + SHIELD_GAP);
            shields.add(new Shield(new Rectangle(shieldX, SHIELD_Y, SHIELD_WIDTH, SHIELD_HEIGHT), 3));
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
        updateUfo();
        updateAnimationFrame();
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

    public List<Rectangle> getShieldRectangles() {
        List<Rectangle> copy = new ArrayList<>();
        for (Shield shield : shields) {
            copy.add(new Rectangle(shield.bounds));
        }
        return Collections.unmodifiableList(copy);
    }

    public List<Shield> getShields() {
        return Collections.unmodifiableList(shields);
    }

    public Rectangle getUfo() {
        return ufo == null ? null : new Rectangle(ufo);
    }

    public boolean isAnimFrame() {
        return animFrame;
    }

    public int getScore() {
        return score;
    }

    public int getLives() {
        return lives;
    }

    public int getRecommendedTimerInterval() {
        return recommendedTimerIntervalMs;
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
        handlePlayerBulletShieldCollisions();
        handlePlayerBulletUfoCollision();
        handlePlayerBulletAlienCollision();
        handleAlienBulletShieldCollisions();
        handleAlienPlayerCollisions();
        handleAlienBulletPlayerCollisions();
    }

    private void handlePlayerBulletShieldCollisions() {
        if (playerBullet == null) {
            return;
        }

        for (int index = shields.size() - 1; index >= 0; index--) {
            Shield shield = shields.get(index);
            if (!intersects(playerBullet, shield.bounds.x, shield.bounds.y, shield.bounds.width, shield.bounds.height)) {
                continue;
            }

            damageShield(index);
            playerBullet = null;
            return;
        }
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
                    registerAlienDestroyed();
                    return;
                }
            }
        }
    }

    private void handlePlayerBulletUfoCollision() {
        if (playerBullet == null || ufo == null) {
            return;
        }

        if (intersects(playerBullet, ufo.x, ufo.y, ufo.width, ufo.height)) {
            score += SCORE_PER_UFO;
            playerBullet = null;
            ufo = null;
            ufoSpawnCooldownTicks = random.nextInt(UFO_SPAWN_MAX_COOLDOWN - UFO_SPAWN_MIN_COOLDOWN + 1)
                    + UFO_SPAWN_MIN_COOLDOWN;
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

    private void handleAlienPlayerCollisions() {
        Rectangle playerBounds = new Rectangle(playerX, PLAYER_Y, PLAYER_WIDTH, PLAYER_HEIGHT);

        for (int row = 0; row < ALIEN_ROWS; row++) {
            for (int col = 0; col < ALIEN_COLS; col++) {
                if (!aliensAlive[row][col]) {
                    continue;
                }

                int alienX = aliensOriginX + col * (ALIEN_WIDTH + ALIEN_H_SPACING);
                int alienY = aliensOriginY + row * (ALIEN_HEIGHT + ALIEN_V_SPACING);
                Rectangle alienBounds = new Rectangle(alienX, alienY, ALIEN_WIDTH, ALIEN_HEIGHT);

                if (alienBounds.intersects(playerBounds)) {
                    lives = 0;
                    playerBullet = null;
                    alienBullets.clear();
                    return;
                }
            }
        }
    }

    private void handleAlienBulletShieldCollisions() {
        for (int bulletIndex = alienBullets.size() - 1; bulletIndex >= 0; bulletIndex--) {
            Bullet bullet = alienBullets.get(bulletIndex);

            boolean bulletConsumed = false;
            for (int shieldIndex = shields.size() - 1; shieldIndex >= 0; shieldIndex--) {
                Shield shield = shields.get(shieldIndex);
                if (!intersects(bullet, shield.bounds.x, shield.bounds.y, shield.bounds.width, shield.bounds.height)) {
                    continue;
                }

                damageShield(shieldIndex);
                alienBullets.remove(bulletIndex);
                bulletConsumed = true;
                break;
            }

            if (bulletConsumed) {
                continue;
            }
        }
    }

    private void updateUfo() {
        if (ufo == null) {
            if (ufoSpawnCooldownTicks > 0) {
                ufoSpawnCooldownTicks--;
                return;
            }

            spawnUfo();
            return;
        }

        ufo.x += ufoDirectionX * UFO_SPEED_X;
        if ((ufoDirectionX > 0 && ufo.x > WORLD_WIDTH) || (ufoDirectionX < 0 && ufo.x + ufo.width < 0)) {
            despawnUfo();
        }
    }

    private void spawnUfo() {
        boolean moveRight = random.nextBoolean();
        int startX = moveRight ? -UFO_WIDTH : WORLD_WIDTH;
        ufoDirectionX = moveRight ? 1 : -1;
        ufo = new Rectangle(startX, UFO_Y, UFO_WIDTH, UFO_HEIGHT);
    }

    private void despawnUfo() {
        ufo = null;
        ufoSpawnCooldownTicks = random.nextInt(UFO_SPAWN_MAX_COOLDOWN - UFO_SPAWN_MIN_COOLDOWN + 1)
                + UFO_SPAWN_MIN_COOLDOWN;
    }

    private void updateAnimationFrame() {
        long now = System.currentTimeMillis();
        while (now - lastAnimToggleTimeMs >= ANIM_FRAME_TOGGLE_MS) {
            animFrame = !animFrame;
            lastAnimToggleTimeMs += ANIM_FRAME_TOGGLE_MS;
        }
    }

    private void damageShield(int shieldIndex) {
        Shield shield = shields.get(shieldIndex);
        shield.health--;
        if (shield.health <= 0) {
            shields.remove(shieldIndex);
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

    private void registerAlienDestroyed() {
        destroyedAliens++;
        recommendedTimerIntervalMs = Math.max(
                MIN_TIMER_INTERVAL_MS,
                BASE_TIMER_INTERVAL_MS - destroyedAliens * TIMER_INTERVAL_STEP_MS
        );
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

    public static class Shield {
        private final Rectangle bounds;
        private int health;

        public Shield(Rectangle bounds, int health) {
            this.bounds = bounds;
            this.health = health;
        }

        public Rectangle getBounds() {
            return new Rectangle(bounds);
        }

        public int getHealth() {
            return health;
        }
    }
}
