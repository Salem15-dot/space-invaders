import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class GameController {
    private final GameModel model;
    private final GameView view;

    // TODO: Coordinate input, game loop updates, and communication between model and view.
    public GameController(GameModel model, GameView view) {
        this.model = model;
        this.view = view;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameModel model = new GameModel();
            GameView view = new GameView();
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
