package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BallTest {

    App app;
    Ball ball;

    @BeforeEach
    void setup() {
        app = new App();
        PApplet.runSketch(new String[]{"App"}, app);
        app.configPath = "config.json";
        app.setup();
        app.loadLevelConfig(0); // Load the initial level configuration for testing

        // Initialize a ball at a sample position with a default velocity
        ball = new Ball(100, 100, 2, 3, app.greyBallImage, app);
    }

    @Test
    void testBallInitialization() {
        assertEquals(100, ball.x, "Initial X position of the ball should be set correctly.");
        assertEquals(100, ball.y, "Initial Y position of the ball should be set correctly.");
        assertEquals(2, ball.vx, "Initial X velocity should be set correctly.");
        assertEquals(3, ball.vy, "Initial Y velocity should be set correctly.");
        assertNotNull(ball.image, "Ball image should be initialized.");
    }

    @Test
    void testBallUpdate() {
        ball.update();
        assertEquals(102, ball.x, "X position should be updated based on velocity.");
        assertEquals(103, ball.y, "Y position should be updated based on velocity.");
    }

    @Test
    void testBallAcceleration() {
        float initialVx = ball.vx;
        float initialVy = ball.vy;
        ball.accelerate();
        assertTrue(ball.vx > initialVx, "X velocity should increase after acceleration.");
        assertTrue(ball.vy > initialVy, "Y velocity should increase after acceleration.");
    }

    @Test
    void testBallLineCollision() {
        PVector startPoint = new PVector(90, 100);
        PVector endPoint = new PVector(110, 100);
        app.playerLines.add(new PVector[]{startPoint, endPoint});
        ball.update();
        ball.handleLineCollision(app.playerLines);
        assertEquals(-2, ball.vx, "X velocity should reflect after collision.");
        assertEquals(3, ball.vy, "Y velocity should remain the same after horizontal line collision.");
    }

    @Test
    void testBallNoLineCollision() {
        // Create a line that the ball shouldn't collide with
        PVector startPoint = new PVector(150, 150);
        PVector endPoint = new PVector(160, 150);
        app.playerLines.add(new PVector[]{startPoint, endPoint});

        // Update the ball's position such that it doesn't collide
        ball.update();
        ball.handleLineCollision(app.playerLines);

        // Check that velocities remain the same
        assertEquals(2, ball.vx, "X velocity should remain unchanged when no collision occurs.");
        assertEquals(3, ball.vy, "Y velocity should remain unchanged when no collision occurs.");
    }

    @Test
    void testBallWallCollision() {
        String[] layout = {
                "XXXXXXXXXXXXXXXXXXXX",
                "X                  X",
                "X   1              X",
                "X                  X",
                "XXXXXXXXXXXXXXXXXXXX"
        };
        ball.x = 64;
        ball.y = 64;
        ball.vx = 2;
        ball.vy = 0;
        ball.handleWallCollision(layout);
        assertEquals(-2, ball.vx, "Ball X velocity should invert on hitting a wall.");
    }

    @Test
    void testBallColorChange() {
        char wallType = '2';  // Blue wall
        ball.changeBallColorBasedOnWall(wallType);
        assertNotNull(ball.image, "Ball image should change on hitting a wall.");
        assertEquals(app.blueBallImage.width, ball.image.width, "Ball image width should match blue ball.");
        assertEquals(app.blueBallImage.height, ball.image.height, "Ball image height should match blue ball.");
    }

    @Test
    void testBallColorChangeOnDifferentWalls() {
        // Test all wall types (0 to 4)
        for (int wallType = 0; wallType <= 4; wallType++) {
            ball.changeBallColorBasedOnWall((char) ('0' + wallType));
            assertNotNull(ball.image, "Ball image should change for wall type " + wallType);
        }
    }

    @Test
    void testBallBoundaryCollisionHandling() {
        // Ball hits the top boundary
        ball.y = App.TOPBAR - 5;  // Above the top boundary
        ball.vy = -3;  // Moving upwards
        ball.handleWallCollision(app.currentLayout);
        assertEquals(3, ball.vy, "Ball Y velocity should invert when hitting the top boundary.");

        // Ball hits the bottom boundary
        ball.y = App.HEIGHT - 5;  // Below the bottom boundary
        ball.vy = 3;  // Moving downwards
        ball.handleWallCollision(app.currentLayout);
        assertEquals(-3, ball.vy, "Ball Y velocity should invert when hitting the bottom boundary.");
    }

    @Test
    void testBallOutOfBoundsHandling() {
        ball.x = -10;  // Move the ball out of bounds
        ball.handleWallCollision(app.currentLayout);
        assertEquals(0, ball.x, "Ball should adjust its position to stay within bounds.");

        ball.x = app.WIDTH + 10;  // Move the ball out of bounds
        ball.handleWallCollision(app.currentLayout);
        assertEquals(app.WIDTH - App.CELLSIZE, ball.x, "Ball should adjust its position to stay within bounds.");
    }

    @Test
    void testGreyBallRespawn() {
        app.spawners.add(new int[]{1, 1});
        app.spawners.add(new int[]{3, 3});

        // Create a grey ball and check if it respawns at a valid spawner
        ball.image = app.greyBallImage;
        boolean respawned = app.respawnBallAtSpawner(ball);

        assertTrue(respawned, "Grey ball should respawn at a spawner.");
        assertTrue((ball.x == 32 || ball.x == 96) && (ball.y == 97 || ball.y == 193),
                   "Ball should be positioned at one of the spawner locations after respawn.");
    }

    @Test
    void testScoreIncreaseForDifferentBallColors() {
        app.levelMultiplier = 1;  // Disable multiplier for simple testing
        int initialScore = app.score;

        // Test for each color and verify score increase
        app.orangeBallImage = new PImage();
        ball.image = app.orangeBallImage;
        app.checkHoleAbsorption(ball);
        assertEquals(initialScore + app.orangePoints, app.score, "Score should increase by orange points.");

        app.score = initialScore;  // Reset score
        app.blueBallImage = new PImage();
        ball.image = app.blueBallImage;
        app.checkHoleAbsorption(ball);
        assertEquals(initialScore + app.bluePoints, app.score, "Score should increase by blue points.");
    }

    @Test
    void testScoreDeductionOnFailedAbsorption() {
        int initialScore = app.score;
        app.penaltyPoints = 5;

        // Ball with a different color than the hole
        ball.image = app.orangeBallImage;
        boolean absorbed = app.checkHoleAbsorption(ball);

        assertFalse(absorbed, "Ball should not be absorbed with color mismatch.");
        assertEquals(initialScore - app.penaltyPoints, app.score, "Score should decrease by penalty points on failed absorption.");
    }

    @Test
    void testBallAccelerationTileCollision() {
        // Create a layout with an acceleration tile
        app.currentLayout = new String[]{
            "XXXXXX",
            "X Q  X",  // Q represents an acceleration tile
            "XXXXXX"
        };

        // Place the ball on the acceleration tile
        ball.x = 32;
        ball.y = 64;
        ball.vx = 2;
        ball.vy = 3;

        // Test the collision with acceleration tile
        app.checkAccelerationTileCollision(ball);
        assertTrue(ball.vx > 2 && ball.vy > 3, "Ball velocity should increase after hitting acceleration tile.");
    }
    

    @Test
    void testBallDisplay() {
        // Mock the display call to ensure it doesn't throw errors
        ball.display(app);

        // No assert needed, this is mainly to verify no exceptions are thrown
    }
}
