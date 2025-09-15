package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    App app;

    @BeforeEach
    void setup() {
        app = new App();
        PApplet.runSketch(new String[] {"App"}, app);
        app.configPath = "config.json"; // Ensure the path points to a valid JSON configuration
        app.setup();
    }

    @Test
    void testLoadLevelConfig() {
        app.loadLevelConfig(0); // Load the first level
        assertNotNull(app.currentLayout, "Level layout should be loaded.");
        assertTrue(app.currentLayout.length > 0, "Level layout should not be empty.");
        assertTrue(app.remainingTime > 0, "Remaining time should be initialized.");
    }

    @Test
    void testRestartLevel() {
        app.loadLevelConfig(0); // Load the first level
        app.remainingTime = 0;
        app.levelEnded = true;
        app.restartLevel();
        assertFalse(app.levelEnded, "Level should be reset.");
        assertEquals(app.totalTime, app.remainingTime, "Remaining time should reset to total time.");
    }

    @Test
    void testDrawSpeedBoost() {
        app.loadLevelConfig(0);
        app.drawSpeedBoost(5, 5);
        assertNotNull(app.speedImage, "Speed image should be loaded.");
    }

    @Test
    void testBallAbsorption() {
        app.loadLevelConfig(0);
        app.score = 0;
        Ball ball = new Ball(100, 100, 1, 1, app.greyBallImage, app);
        boolean absorbed = app.checkHoleAbsorption(ball);
        assertFalse(absorbed, "Grey ball should not be absorbed without hitting a hole.");
    }

    @Test
    void testSpawnerHandling() {
        app.loadLevelConfig(0);
        assertFalse(app.spawners.isEmpty(), "Spawners should be identified and loaded.");
    }

    @Test
    void testKeyPressedR() {
        app.loadLevelConfig(0);
        app.key = 'r';
        app.keyPressed();
        assertFalse(app.levelEnded, "Level should restart on pressing 'r'.");
    }

    @Test
    void testScoreUpdateForAbsorption() {
        app.loadLevelConfig(0);
        Ball ball = new Ball(32, 32, 0, 0, app.orangeBallImage, app);
        app.score = 0;
        app.checkHoleAbsorption(ball);
        assertEquals(app.orangePoints * app.levelMultiplier, app.score, "Score should update correctly after ball absorption.");
    }
}
