import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class GameController {
    private final GameModel model;
    private final GameView view;
    private final Timer gameTimer;

    private boolean movingLeft;
    private boolean movingRight;

    public GameController(GameModel model, GameView view) {
        this.model = model;
        this.view = view;

        configureInput();
        gameTimer = new Timer(model.getRecommendedTimerInterval(), this::onTick);
        gameTimer.start();
    }

    private void configureInput() {
        InputMap inputMap = view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(KeyStroke.getKeyStroke("pressed LEFT"), "move-left-pressed");
        inputMap.put(KeyStroke.getKeyStroke("released LEFT"), "move-left-released");
        inputMap.put(KeyStroke.getKeyStroke("pressed RIGHT"), "move-right-pressed");
        inputMap.put(KeyStroke.getKeyStroke("released RIGHT"), "move-right-released");
        inputMap.put(KeyStroke.getKeyStroke("pressed SPACE"), "fire");
        inputMap.put(KeyStroke.getKeyStroke("pressed R"), "reset");

        view.getActionMap().put("move-left-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingLeft = true;
            }
        });

        view.getActionMap().put("move-left-released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingLeft = false;
            }
        });

        view.getActionMap().put("move-right-pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingRight = true;
            }
        });

        view.getActionMap().put("move-right-released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingRight = false;
            }
        });

        view.getActionMap().put("fire", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.firePlayerBullet();
            }
        });

        view.getActionMap().put("reset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.reset();
                gameTimer.setDelay(model.getRecommendedTimerInterval());
                gameTimer.restart();
            }
        });
    }

    private void onTick(ActionEvent e) {
        if (movingLeft && !movingRight) {
            model.movePlayerLeft();
        } else if (movingRight && !movingLeft) {
            model.movePlayerRight();
        }

        model.tick();
        view.repaint();
        gameTimer.setDelay(model.getRecommendedTimerInterval());

        if (model.getLives() <= 0) {
            gameTimer.stop();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameModel model = new GameModel();
            GameView view = new GameView(model);
            new GameController(model, view);

            JFrame frame = new JFrame("Space Invaders");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setContentPane(view);
            frame.setVisible(true);
        });
    }
}
