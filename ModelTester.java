import java.lang.reflect.Field;
import java.util.List;

public class ModelTester {
    private static int passCount;
    private static int failCount;

    public static void main(String[] args) throws Exception {
        testPlayerCannotMovePastLeftEdge();
        testPlayerCannotMovePastRightEdge();
        testCannotFireSecondBulletWhileFirstInFlight();
        testPlayerBulletRemovedAtTop();
        testDestroyingAlienIncreasesScore();
        testLosingAllLivesTriggersGameOver();

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

    private static void setPrivateField(GameModel model, String fieldName, Object value) throws Exception {
        Field field = GameModel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(model, value);
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
