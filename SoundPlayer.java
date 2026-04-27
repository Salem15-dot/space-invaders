import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public final class SoundPlayer {
    private static final Map<String, Clip> CLIP_CACHE = new ConcurrentHashMap<>();

    private SoundPlayer() {
    }

    public static void play(String soundPath) {
        Clip clip = CLIP_CACHE.computeIfAbsent(soundPath, SoundPlayer::loadClip);
        if (clip == null) {
            return;
        }

        synchronized (clip) {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        }
    }

    private static Clip loadClip(String soundPath) {
        try (AudioInputStream audioStream = openAudioInputStream(soundPath)) {
            if (audioStream == null) {
                return null;
            }

            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (Exception ex) {
            System.err.println("Unable to load sound: " + soundPath + " (" + ex.getMessage() + ")");
            return null;
        }
    }

    private static AudioInputStream openAudioInputStream(String soundPath) {
        try {
            File soundFile = new File(soundPath);
            if (soundFile.exists()) {
                return AudioSystem.getAudioInputStream(soundFile);
            }

            String normalizedPath = soundPath.startsWith("/") ? soundPath.substring(1) : soundPath;
            InputStream resourceStream = SoundPlayer.class.getClassLoader().getResourceAsStream(normalizedPath);
            if (resourceStream != null) {
                return AudioSystem.getAudioInputStream(new BufferedInputStream(resourceStream));
            }
        } catch (Exception ex) {
            System.err.println("Unable to open sound stream: " + soundPath + " (" + ex.getMessage() + ")");
        }

        return null;
    }
}
