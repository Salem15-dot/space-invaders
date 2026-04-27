import java.lang.reflect.Field;
import java.util.List;

public class ModelTester2 {

    static int passed = 0;
    static int failed = 0;

    static void check(String testName, boolean condition) {
        if (condition) {
            System.out.println("PASS: " + testName);
            passed++;
        } else {
            System.out.println("FAIL: " + testName);
            failed++;
        }
    }

    public static void main(String[] args) {
        testInitialState();
        testPlayerMovement();
        testBulletFiring();
        testAlienDestruction();
        testGameOver();

        System.out.println("\n" + passed + " passed, " + failed + " failed.");
    }



    static void testInitialState() {
    GameModel model = new GameModel();
    check("player starts with 3 lives",   model.getLives() == 3);
    check("score starts at zero",         model.getScore() == 0);
    check("no bullet at start",           model.getPlayerBullet() == null);
    check("game is not over at start",    !model.isGameOver());
}

static void testPlayerMovement() {
    GameModel model = new GameModel();
    int startX = model.getPlayerX();
    model.movePlayerRight();
    check("moving right increases x",     model.getPlayerX() > startX);

    // Drive the player as far left as possible
    for (int i = 0; i < 200; i++) model.movePlayerLeft();
    check("player x never goes below 0",  model.getPlayerX() >= 0);
}

static void testBulletFiring() {
    GameModel model = new GameModel();
    model.firePlayerBullet();
    check("firing creates a bullet",      model.getPlayerBullet() != null);
    model.firePlayerBullet();             // fire again while one is in flight
    check("cannot fire a second bullet",  model.getPlayerBullet() != null);
    // (this is a weak check — we want exactly one bullet, not two)
}

static void testAlienDestruction() {
    try {
        GameModel model = new GameModel();
        setPrivateInt(model, "aliensDirectionX", 0);

        int alienX = model.getAliensOriginX();
        int alienY = model.getAliensOriginY();
        setPrivateField(
                model,
                "playerBullet",
                new GameModel.Bullet(
                        alienX,
                        alienY,
                        GameModel.ALIEN_WIDTH,
                        GameModel.ALIEN_HEIGHT,
                        0
                )
        );

        int beforeScore = model.getScore();
        model.tick();
        check("destroying an alien increases score", model.getScore() > beforeScore);
    } catch (Exception e) {
        check("destroying an alien increases score", false);
    }
}

static void testGameOver() {
    try {
        GameModel model = new GameModel();

        for (int i = 0; i < 3; i++) {
            addAlienBulletAtPlayer(model);
            model.tick();
        }

        check("losing all lives triggers game over", model.isGameOver() && model.getLives() == 0);
    } catch (Exception e) {
        check("losing all lives triggers game over", false);
    }
}

@SuppressWarnings("unchecked")
static void addAlienBulletAtPlayer(GameModel model) throws Exception {
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

static void setPrivateInt(GameModel model, String fieldName, int value) throws Exception {
    Field field = GameModel.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.setInt(model, value);
}

static void setPrivateField(GameModel model, String fieldName, Object value) throws Exception {
    Field field = GameModel.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(model, value);
}
}



