import java.lang.reflect.Field;
import java.util.List;

public class ModelTester {
    private static int passCount;
    private static int failCount;

    public static void main(String[] args) throws Exception {
        testPlayerBoundary();
        testPlayerCannotMovePastLeftEdge();
        testPlayerCannotMovePastRightEdge();
        testCannotFireSecondBulletWhileFirstInFlight();
        testPlayerBulletRemovedAtTop();
        testDestroyingAlienIncreasesScore();
        testLosingAllLivesTriggersGameOver();
        testResetRestoresDefaultStartingState();
        testUfoDespawnsAtEdge();
        testUfoCollisionAddsThreeHundredPoints();
        testAlienAnimationFrameTogglesAfterDelay();

        System.out.println();
        System.out.println("Summary: " + passCount + " passed, " + failCount + " failed.");
    }

    private static void testPlayerCannotMovePastLeftEdge() {
        GameModel model = new GameModel();

        for (int i = 0; i < 200; i++) {
            model.movePlayerLeft();
        }

        assertCondition(
                "Player cannot move past left edge",
                model.getPlayerX() == 0,
                "Expected x=0 but was x=" + model.getPlayerX()
        );
    }

    private static void testPlayerBoundary() {
        GameModel model = new GameModel();

        for (int i = 0; i < 200; i++) {
            model.movePlayerLeft();
        }

        assertCondition(
                "Player x never goes below zero after repeated left movement",
                model.getPlayerX() >= 0,
                "Expected x to stay at or above 0 but was x=" + model.getPlayerX()
        );
    }

    private static void testPlayerCannotMovePastRightEdge() {
        GameModel model = new GameModel();

        for (int i = 0; i < 200; i++) {
            model.movePlayerRight();
        }

        int expected = GameModel.WORLD_WIDTH - GameModel.PLAYER_WIDTH;
        assertCondition(
                "Player cannot move past right edge",
                model.getPlayerX() == expected,
                "Expected x=" + expected + " but was x=" + model.getPlayerX()
        );
    }

    private static void testCannotFireSecondBulletWhileFirstInFlight() {
        GameModel model = new GameModel();

        model.firePlayerBullet();
        GameModel.Bullet first = model.getPlayerBullet();

        model.firePlayerBullet();
        GameModel.Bullet secondCallResult = model.getPlayerBullet();

        assertCondition(
                "Firing while bullet in flight does nothing",
                first != null && first == secondCallResult,
                "Expected same bullet instance to remain active"
        );
    }

    private static void testPlayerBulletRemovedAtTop() {
        GameModel model = new GameModel();

        for (int i = 0; i < 200; i++) {
            model.movePlayerLeft();
        }

        model.firePlayerBullet();

        int safety = 200;
        while (model.getPlayerBullet() != null && safety-- > 0) {
            model.tick();
        }

        assertCondition(
                "Player bullet is removed after reaching top",
                model.getPlayerBullet() == null,
                "Expected no active player bullet"
        );
    }

    private static void testDestroyingAlienIncreasesScore() throws Exception {
        GameModel model = new GameModel();

        setPrivateIntField(model, "aliensDirectionX", 0);

        int row = 0;
        int col = 0;
        int alienX = model.getAliensOriginX() + col * (GameModel.ALIEN_WIDTH + GameModel.ALIEN_H_SPACING);
        int alienY = model.getAliensOriginY() + row * (GameModel.ALIEN_HEIGHT + GameModel.ALIEN_V_SPACING);

        GameModel.Bullet guaranteedHit = new GameModel.Bullet(
                alienX,
                alienY,
                GameModel.ALIEN_WIDTH,
                GameModel.ALIEN_HEIGHT,
                0
        );
        setPrivateField(model, "playerBullet", guaranteedHit);

        int before = model.getScore();
        model.tick();
        int after = model.getScore();

        assertCondition(
                "Destroying an alien increases score",
                after == before + 10 && !model.isAlienAlive(row, col),
                "Expected score +10 and alien removed, before=" + before + " after=" + after
        );
    }

    private static void testLosingAllLivesTriggersGameOver() throws Exception {
        GameModel model = new GameModel();

        for (int i = 0; i < 3; i++) {
            addAlienBulletAtPlayer(model);
            model.tick();
        }

        assertCondition(
                "Losing all lives triggers game-over state",
                model.getLives() == 0 && model.isGameOver(),
                "Expected lives=0 and isGameOver=true but got lives=" + model.getLives()
         );
     }

    private static void testResetRestoresDefaultStartingState() throws Exception {
        GameModel model = new GameModel();

        setPrivateIntField(model, "score", 100);
        setPrivateIntField(model, "lives", 0);
        setPrivateIntField(model, "playerX", 0);
        setPrivateIntField(model, "aliensOriginX", 0);
        setPrivateIntField(model, "aliensOriginY", 0);
        setPrivateIntField(model, "destroyedAliens", 12);
        setPrivateIntField(model, "recommendedTimerIntervalMs", 6);
        setPrivateField(model, "playerBullet", new GameModel.Bullet(0, 0, 1, 1, 0));

        @SuppressWarnings("unchecked")
        List<GameModel.Bullet> alienBullets = (List<GameModel.Bullet>) getPrivateField(model, "alienBullets");
        alienBullets.clear();
        alienBullets.add(new GameModel.Bullet(0, 0, 1, 1, 0));

        boolean[][] aliens = (boolean[][]) getPrivateField(model, "aliensAlive");
        for (int row = 0; row < aliens.length; row++) {
            for (int col = 0; col < aliens[row].length; col++) {
                aliens[row][col] = false;
            }
        }

        @SuppressWarnings("unchecked")
        List<GameModel.Shield> shields = (List<GameModel.Shield>) getPrivateField(model, "shields");
        shields.clear();

        model.reset();

        GameModel freshModel = new GameModel();
        boolean aliensRestored = true;
        for (int row = 0; row < GameModel.ALIEN_ROWS; row++) {
            for (int col = 0; col < GameModel.ALIEN_COLS; col++) {
                aliensRestored &= model.isAlienAlive(row, col);
            }
        }

        assertCondition("reset restores score to zero", model.getScore() == 0, "Expected score 0 but was " + model.getScore());
        assertCondition("reset restores lives to three", model.getLives() == 3, "Expected lives 3 but was " + model.getLives());
        assertCondition("reset clears player bullet", model.getPlayerBullet() == null, "Expected player bullet to be null");
        assertCondition("reset clears alien bullets", model.getAlienBullets().isEmpty(), "Expected alien bullets list to be empty");
        assertCondition("reset restores aliens", aliensRestored, "Expected all aliens to be alive again");
        assertCondition("reset restores player position", model.getPlayerX() == freshModel.getPlayerX(), "Expected player x=" + freshModel.getPlayerX() + " but was x=" + model.getPlayerX());
        assertCondition("reset restores recommended timer interval", model.getRecommendedTimerInterval() == freshModel.getRecommendedTimerInterval(), "Expected timer interval " + freshModel.getRecommendedTimerInterval() + " but was " + model.getRecommendedTimerInterval());
    }

    private static void testUfoDespawnsAtEdge() throws Exception {
        GameModel model = new GameModel();

        setPrivateField(model, "ufo", new java.awt.Rectangle(GameModel.WORLD_WIDTH - 1, GameModel.UFO_Y, GameModel.UFO_WIDTH, GameModel.UFO_HEIGHT));
        setPrivateIntField(model, "ufoDirectionX", 1);
        setPrivateIntField(model, "ufoSpawnCooldownTicks", 0);

        model.tick();
        model.tick();

        assertCondition("UFO despawns safely at the opposite edge", model.getUfo() == null, "Expected UFO to be null after leaving screen");
    }

    private static void testUfoCollisionAddsThreeHundredPoints() throws Exception {
        GameModel model = new GameModel();

        setPrivateField(model, "ufo", new java.awt.Rectangle(200, GameModel.UFO_Y, GameModel.UFO_WIDTH, GameModel.UFO_HEIGHT));
        setPrivateIntField(model, "ufoDirectionX", 0);
        setPrivateField(model, "playerBullet", new GameModel.Bullet(200, GameModel.UFO_Y, GameModel.PLAYER_BULLET_WIDTH, GameModel.PLAYER_BULLET_HEIGHT, 0));

        int before = model.getScore();
        model.tick();

        assertCondition("UFO hit adds 300 points", model.getScore() == before + 300, "Expected score increase of 300 but was " + (model.getScore() - before));
        assertCondition("UFO is removed after collision", model.getUfo() == null, "Expected UFO to be removed after collision");
    }

    private static void testAlienAnimationFrameTogglesAfterDelay() {
        GameModel model = new GameModel();
        boolean initialFrame = model.isAnimFrame();

        try {
            setPrivateLongField(
                    model,
                    "lastAnimToggleTimeMs",
                    System.currentTimeMillis() - GameModel.ANIM_FRAME_TOGGLE_MS - 50L
            );
        } catch (Exception e) {
            assertCondition(
                    "Alien animation frame toggles after delay",
                    false,
                    "Unable to adjust animation timing for test"
            );
            return;
        }

        model.tick();

        assertCondition(
                "Alien animation frame toggles after delay",
                model.isAnimFrame() != initialFrame,
                "Expected animFrame to toggle after the animation delay"
        );
    }

    @SuppressWarnings("unchecked")
    private static void addAlienBulletAtPlayer(GameModel model) throws Exception {
        Field bulletsField = GameModel.class.getDeclaredField("alienBullets");
        bulletsField.setAccessible(true);
        List<GameModel.Bullet> bullets = (List<GameModel.Bullet>) bulletsField.get(model);

        bullets.add(new GameModel.Bullet(
                model.getPlayerX(),
                model.getPlayerY(),
                GameModel.ALIEN_BULLET_WIDTH,
                GameModel.ALIEN_BULLET_HEIGHT,
                0
        ));
    }

    private static void setPrivateIntField(GameModel model, String fieldName, int value) throws Exception {
        Field field = GameModel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(model, value);
    }

    private static void setPrivateLongField(GameModel model, String fieldName, long value) throws Exception {
        Field field = GameModel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setLong(model, value);
    }

    private static void setPrivateField(GameModel model, String fieldName, Object value) throws Exception {
        Field field = GameModel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(model, value);
    }

    private static Object getPrivateField(GameModel model, String fieldName) throws Exception {
        Field field = GameModel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(model);
    }

    private static void assertCondition(String testName, boolean condition, String failureDetails) {
        if (condition) {
            passCount++;
            System.out.println("PASS: " + testName);
        } else {
            failCount++;
            System.out.println("FAIL: " + testName + " | " + failureDetails);
        }
    }
}
