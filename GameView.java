import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import javax.swing.JPanel;

public class GameView extends JPanel {
    private final GameModel model;

    public GameView(GameModel model) {
        this.model = model;
        setPreferredSize(new Dimension(GameModel.WORLD_WIDTH, GameModel.WORLD_HEIGHT));
        setBackground(new Color(10, 10, 20));
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        drawPlayer(g2);
        drawAliens(g2);
        for (GameModel.Shield shield : model.getShields()) {
            java.awt.Rectangle bounds = shield.getBounds();
            double healthRatio = Math.max(0.0, Math.min(1.0, shield.getHealth() / 3.0));
            int red = (int) Math.round(140 * (1.0 - healthRatio));
            int green = (int) Math.round(255 * healthRatio);
            int blue = (int) Math.round(40 * (1.0 - healthRatio));
            g2.setColor(new Color(red, green, blue));
            g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        drawUfo(g2);
        drawBullets(g2);
        drawHud(g2);
        drawGameOver(g2);
    }

    private void drawPlayer(Graphics2D g2) {
        g2.setColor(new Color(110, 255, 130));
        g2.fillRect(
                model.getPlayerX(),
                model.getPlayerY(),
                GameModel.PLAYER_WIDTH,
                GameModel.PLAYER_HEIGHT
        );
    }

    private void drawAliens(Graphics2D g2) {
        int originX = model.getAliensOriginX();
        int originY = model.getAliensOriginY();

        g2.setColor(new Color(255, 235, 100));
        for (int row = 0; row < GameModel.ALIEN_ROWS; row++) {
            for (int col = 0; col < GameModel.ALIEN_COLS; col++) {
                if (!model.isAlienAlive(row, col)) {
                    continue;
                }

                int x = originX + col * (GameModel.ALIEN_WIDTH + GameModel.ALIEN_H_SPACING);
                int y = originY + row * (GameModel.ALIEN_HEIGHT + GameModel.ALIEN_V_SPACING);
                g2.fillRect(x, y, GameModel.ALIEN_WIDTH, GameModel.ALIEN_HEIGHT);
            }
        }
    }

    private void drawUfo(Graphics2D g2) {
        java.awt.Rectangle ufo = model.getUfo();
        if (ufo == null) {
            return;
        }

        g2.setColor(new Color(255, 90, 220));
        g2.fillRect(ufo.x, ufo.y, ufo.width, ufo.height);
    }

    private void drawBullets(Graphics2D g2) {
        GameModel.Bullet playerBullet = model.getPlayerBullet();
        if (playerBullet != null) {
            g2.setColor(new Color(140, 230, 255));
            g2.fillRect(
                    playerBullet.getX(),
                    playerBullet.getY(),
                    playerBullet.getWidth(),
                    playerBullet.getHeight()
            );
        }

        List<GameModel.Bullet> alienBullets = model.getAlienBullets();
        g2.setColor(new Color(255, 110, 110));
        for (GameModel.Bullet bullet : alienBullets) {
            g2.fillRect(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight());
        }
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Score: " + model.getScore(), 16, 28);
        g2.drawString("Lives: " + model.getLives(), GameModel.WORLD_WIDTH - 100, 28);
    }

    private void drawGameOver(Graphics2D g2) {
        if (model.getLives() > 0) {
            return;
        }

        String message = "GAME OVER";
        g2.setFont(new Font("SansSerif", Font.BOLD, 56));
        FontMetrics metrics = g2.getFontMetrics();
        int textWidth = metrics.stringWidth(message);
        int x = (getWidth() - textWidth) / 2;
        int y = getHeight() / 2;

        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(255, 120, 120));
        g2.drawString(message, x, y);
    }
}
