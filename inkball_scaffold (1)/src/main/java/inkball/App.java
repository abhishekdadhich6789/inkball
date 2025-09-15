package inkball;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import processing.data.JSONArray;
import processing.data.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class App extends PApplet {

    public static final int CELLSIZE = 32;
    public static final int TOPBAR = 64;
    public static int WIDTH = 576;
    public static int HEIGHT = 640;
    public static final int FPS = 30;

    public PImage greyBallImage;
    public PImage orangeBallImage;
    public PImage blueBallImage;
    public PImage greenBallImage;
    public PImage yellowBallImage;

    public PImage upArrowImage;
    public PImage downArrowImage;
    public PImage leftArrowImage;
    public PImage rightArrowImage;

    public PImage speedImage;

    public String configPath;
    public int spawnInterval;
    public String layoutFile;
    public int level = 1;
    public int totalTime;
    public int remainingTime;
    public int score = 0;
    public boolean levelEnded = false;

    public int greyPoints;
    public int orangePoints;
    public int bluePoints;
    public int greenPoints;
    public int yellowPoints;
    public int penaltyPoints;
    public int levelMultiplier = 1;

    public int yOffset = 65;

    public String[] currentLayout;

    public PImage[] wallImages = new PImage[5];
    public PImage[] holeImages = new PImage[5];
    public PImage spawnerImage;

    public int frameCounter = 0;
    public int spawnCounter;

    List<int[]> spawners = new ArrayList<>();
    List<Ball> balls = new ArrayList<>();
    List<PImage> upcomingBallImages = new ArrayList<>();
    Random random = new Random();

    List<PVector[]> playerLines = new ArrayList<>();

    public App() {
        this.configPath = "config.json";
    }

    @Override
    public void settings() {
        size(WIDTH, HEIGHT);
    }

    @Override
    public void setup() {
        frameRate(FPS);

        wallImages[0] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/wall0.png");
        wallImages[1] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/wall1.png");
        wallImages[2] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/wall2.png");
        wallImages[3] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/wall3.png");
        wallImages[4] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/wall4.png");
        spawnerImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/entrypoint.png");

        holeImages[0] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/hole0.png");
        holeImages[1] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/hole1.png");
        holeImages[2] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/hole2.png");
        holeImages[3] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/hole3.png");
        holeImages[4] = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/hole4.png");

        greyBallImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball0.png");
        orangeBallImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball1.png");
        blueBallImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball2.png");
        greenBallImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball3.png");
        yellowBallImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball4.png");

        upArrowImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/upArrow.png");
        downArrowImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/downArrow.png");
        leftArrowImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/leftArrow.png");
        rightArrowImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/rightArrow.png");

        speedImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/speed.png");
        loadLevelConfig(level);
    }

    public void loadLevelConfig(int levelIndex) {
        JSONObject config = loadJSONObject(configPath);
        JSONArray levels = config.getJSONArray("levels");
        if (levelIndex < levels.size()) {
            JSONObject selectedLevel = levels.getJSONObject(levelIndex);
            layoutFile = selectedLevel.getString("layout");
            spawnInterval = selectedLevel.getInt("spawn_interval");
            totalTime = selectedLevel.getInt("time", -1);
            remainingTime = totalTime;
            spawnCounter = spawnInterval * FPS;

            JSONArray ballsArray = selectedLevel.getJSONArray("balls");
            upcomingBallImages.clear();
            for (int i = 0; i < ballsArray.size(); i++) {
                String ballColor = ballsArray.getString(i);
                String ballImageFile = getBallImageFilename(ballColor);
                PImage ballImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/" + ballImageFile);
                upcomingBallImages.add(ballImage);
            }

            greyPoints = selectedLevel.getInt("grey_points", 5);
            orangePoints = selectedLevel.getInt("orange_points", 10);
            bluePoints = selectedLevel.getInt("blue_points", 15);
            greenPoints = selectedLevel.getInt("green_points", 20);
            yellowPoints = selectedLevel.getInt("yellow_points", 25);
            penaltyPoints = selectedLevel.getInt("penalty_points", 5);
            levelMultiplier = selectedLevel.getInt("level_multiplier", 1);

            loadLevel(layoutFile);
        } else {
            System.out.println("No more levels available.");
        }
    }

    public void loadLevel(String layoutFile) {
        currentLayout = loadStrings(layoutFile);
        spawners.clear();
        balls.clear();

        for (int row = 0; row < currentLayout.length; row++) {
            String line = currentLayout[row];
            for (int col = 0; col < line.length(); col++) {
                char cell = line.charAt(col);
                if (cell == 'S') {
                    spawners.add(new int[]{col, row});
                } else if (cell == 'B' && col + 1 < line.length() && Character.isDigit(line.charAt(col + 1))) {
                    int ballType = Character.getNumericValue(line.charAt(col + 1));
                    PImage ballImage = loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball" + ballType + ".png");
                    float vx = random.nextBoolean() ? 2 : -2;
                    float vy = random.nextBoolean() ? 2 : -2;
                    balls.add(new Ball(col * CELLSIZE, row * CELLSIZE + yOffset, vx, vy, ballImage, this));
                    col++;
                }
            }
        }

        System.out.println("Spawners found: " + spawners.size());
    }

    @Override
    public void draw() {
        if (levelEnded) {
            fill(0);
            textSize(32);
            text("=== TIME’S UP ===", WIDTH / 2 - 100, 50);
            return;
        }

        background(255);

        frameCounter++;
        if (frameCounter >= FPS) {
            remainingTime--;
            frameCounter = 0;
        }

        if (remainingTime <= 0) {
            levelEnded = true;
            remainingTime = 0;
            noLoop();
        }

        drawWallsAndHoles();
        drawSpawners();
        updateAndDisplayBalls();

        displayTimer();
        displayScore();
        displayUpcomingBalls();

        stroke(0);
        strokeWeight(10);
        for (PVector[] line : playerLines) {
            line(line[0].x, line[0].y, line[1].x, line[1].y);
        }
    }

    public String getBallColorFromImage(PImage ballImage) {
        if (ballImage == null) return null;

        if (compareImages(ballImage, greyBallImage)) return "grey";
        if (compareImages(ballImage, orangeBallImage)) return "orange";
        if (compareImages(ballImage, blueBallImage)) return "blue";
        if (compareImages(ballImage, greenBallImage)) return "green";
        if (compareImages(ballImage, yellowBallImage)) return "yellow";

        return null;
    }

    private boolean compareImages(PImage img1, PImage img2) {
        if (img1.width != img2.width || img1.height != img2.height) {
            return false;
        }

        img1.loadPixels();
        img2.loadPixels();

        for (int i = 0; i < img1.pixels.length; i++) {
            if (img1.pixels[i] != img2.pixels[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void keyPressed() {
        if (key == 'r') {
            restartLevel();
        }
    }

    public void restartLevel() {
        levelEnded = false;
        remainingTime = totalTime;
        playerLines.clear();
        loadLevelConfig(level);
        loop();
    }

    @Override
    public void mouseDragged() {
        if (!levelEnded) {
            PVector startPoint = new PVector(mouseX, mouseY);
            PVector endPoint = new PVector(pmouseX, pmouseY);
            playerLines.add(new PVector[]{startPoint, endPoint});
        }
    }

    public void drawSpeedBoost(int col, int row) {
        if (speedImage != null) {
            PImage resizedSpeedImage = speedImage.copy();
            resizedSpeedImage.resize(CELLSIZE, CELLSIZE);
            image(resizedSpeedImage, col * CELLSIZE, row * CELLSIZE + yOffset);
        } else {
            System.out.println("Speed image not loaded.");
        }
    }

    public void checkAccelerationTileCollision(Ball ball) {
        boolean onAccelerationTile = false;
        for (int row = 0; row < currentLayout.length; row++) {
            String line = currentLayout[row];
            for (int col = 0; col < line.length(); col++) {
                if (line.charAt(col) == 'Q') {
                    float tileX = col * CELLSIZE;
                    float tileY = row * CELLSIZE + yOffset;

                    if (ball.x + CELLSIZE > tileX && ball.x < tileX + CELLSIZE &&
                        ball.y + CELLSIZE > tileY && ball.y < tileY + CELLSIZE) {
                        onAccelerationTile = true;
                        break;
                    }
                }
            }
            if (onAccelerationTile) break;
        }

        if (onAccelerationTile) {
            ball.accelerate();
        }
    }

    public void drawWallsAndHoles() {
        for (int row = 0; row < currentLayout.length; row++) {
            String line = currentLayout[row];
            for (int col = 0; col < line.length(); col++) {
                char cell = line.charAt(col);
                if (cell == 'X') {
                    image(wallImages[0], col * CELLSIZE, row * CELLSIZE + yOffset);
                } else if (Character.isDigit(cell)) {
                    int wallType = Character.getNumericValue(cell);
                    if (wallType >= 1 && wallType <= 4) {
                        image(wallImages[wallType], col * CELLSIZE, row * CELLSIZE + yOffset);
                    }
                } else if (cell == 'H' && col + 1 < line.length() && Character.isDigit(line.charAt(col + 1))) {
                    int holeType = Character.getNumericValue(line.charAt(col + 1));
                    if (holeType >= 0 && holeType <= 4) {
                        image(holeImages[holeType], col * CELLSIZE, row * CELLSIZE + yOffset);
                        col++;
                    }
                } else if (cell == 'u') {
                    image(upArrowImage, col * CELLSIZE, row * CELLSIZE + yOffset);
                } else if (cell == 'd') {
                    image(downArrowImage, col * CELLSIZE, row * CELLSIZE + yOffset);
                } else if (cell == 'l') {
                    image(leftArrowImage, col * CELLSIZE, row * CELLSIZE + yOffset);
                } else if (cell == 'r') {
                    image(rightArrowImage, col * CELLSIZE, row * CELLSIZE + yOffset);
                } else if (cell == 'Q') {
                    drawSpeedBoost(col, row);
                }
            }
        }
    }

    public void drawSpawners() {
        for (int[] spawner : spawners) {
            image(spawnerImage, spawner[0] * CELLSIZE, spawner[1] * CELLSIZE + yOffset);
        }
    }

    public void updateAndDisplayBalls() {
        if (levelEnded) return;

        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball ball = balls.get(i);
            ball.update();
            ball.handleLineCollision(playerLines);
            ball.handleWallCollision(currentLayout);
            checkAccelerationTileCollision(ball);

            if (checkHoleAbsorption(ball)) {
                if (!respawnBallAtSpawner(ball)) {
                    balls.remove(i);
                }
            } else {
                ball.display(this);
            }
        }

        spawnCounter--;
        if (spawnCounter <= 0 && !upcomingBallImages.isEmpty()) {
            int randomSpawnerIndex = random.nextInt(spawners.size());
            int[] spawner = spawners.get(randomSpawnerIndex);
            int spawnX = spawner[0] * CELLSIZE;
            int spawnY = spawner[1] * CELLSIZE + yOffset;
            PImage nextBallImage = upcomingBallImages.remove(0);

            if (nextBallImage == null) {
                System.out.println("Error: Image not found for upcoming ball.");
            } else {
                float vx = random.nextBoolean() ? 2 : -2;
                float vy = random.nextBoolean() ? 2 : -2;
                balls.add(new Ball(spawnX, spawnY, vx, vy, nextBallImage, this));
            }
            spawnCounter = spawnInterval * FPS;
        }
    }

    public void displayTimer() {
        fill(0);
        textSize(18);
        text("Time: " + remainingTime, WIDTH - 150, 50);

        if (levelEnded) {
            text("=== TIME’S UP ===", WIDTH / 2 - 100, 50);
        }
    }

    public void displayScore() {
        fill(0);
        textSize(18);
        text("Score: " + score, 20, 50);
    }

    public void displayUpcomingBalls() {
        for (int i = 0; i < min(5, upcomingBallImages.size()); i++) {
            PImage ballImage = upcomingBallImages.get(i);
            image(ballImage, 150 + (i * 40), 20, 32, 32);
        }
    }

    public boolean respawnBallAtSpawner(Ball ball) {
        String ballColor = getBallColorFromImage(ball.image);
        if (ballColor != null && ballColor.equals("grey")) {
            int randomSpawnerIndex = random.nextInt(spawners.size());
            int[] spawner = spawners.get(randomSpawnerIndex);
            ball.x = spawner[0] * CELLSIZE;
            ball.y = spawner[1] * CELLSIZE + yOffset;
            return true;
        }
        return false;
    }

    public String getHoleColor(int holeType) {
        switch (holeType) {
            case 0: return "grey";
            case 1: return "orange";
            case 2: return "blue";
            case 3: return "green";
            case 4: return "yellow";
            default: return null;
        }
    }

    public boolean checkHoleAbsorption(Ball ball) {
        for (int row = 0; row < currentLayout.length; row++) {
            String line = currentLayout[row];
            for (int col = 0; col < line.length(); col++) {
                char cell = line.charAt(col);
                if (cell == 'H' && col + 1 < line.length() && Character.isDigit(line.charAt(col + 1))) {
                    int holeType = Character.getNumericValue(line.charAt(col + 1));
                    if (holeType >= 0 && holeType <= 4) {
                        float holeX = col * CELLSIZE;
                        float holeY = row * CELLSIZE + yOffset;

                        if (ball.x + CELLSIZE > holeX && ball.x < holeX + CELLSIZE &&
                            ball.y + CELLSIZE > holeY && ball.y < holeY + CELLSIZE) {

                            String ballColor = getBallColorFromImage(ball.image);
                            String holeColor = getHoleColor(holeType);

                            if ((ballColor != null && ballColor.equals(holeColor)) || 
                                ballColor.equals("grey") || holeColor.equals("grey")) {
                                
                                switch (ballColor) {
                                    case "grey":
                                        score += greyPoints * levelMultiplier;
                                        break;
                                    case "orange":
                                        score += orangePoints * levelMultiplier;
                                        break;
                                    case "blue":
                                        score += bluePoints * levelMultiplier;
                                        break;
                                    case "green":
                                        score += greenPoints * levelMultiplier;
                                        break;
                                    case "yellow":
                                        score += yellowPoints * levelMultiplier;
                                        break;
                                }

                                return true;
                            } else {
                                score -= penaltyPoints;
                                upcomingBallImages.add(ball.image);
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public String getBallImageFilename(String ballType) {
        switch (ballType) {
            case "grey":
                return "ball0.png";
            case "orange":
                return "ball1.png";
            case "blue":
                return "ball2.png";
            case "green":
                return "ball3.png";
            case "yellow":
                return "ball4.png";
            default:
                System.out.println("Unknown ball type: " + ballType);
                return "default_ball.png";
        }
    }

    public static void main(String[] args) {
        PApplet.main("inkball.App");
    }
}

class Ball {
    float x, y;
    float vx, vy;
    PImage image;
    PApplet applet;

    private static final float ACCELERATION_FACTOR = 1.1f;

    Ball(float x, float y, float vx, float vy, PImage image, PApplet applet) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.image = image;
        this.applet = applet;
    }

    void update() {
        x += vx;
        y += vy;
    }

    void accelerate() {
        vx *= ACCELERATION_FACTOR;
        vy *= ACCELERATION_FACTOR;
    }

    void handleLineCollision(List<PVector[]> playerLines) {
        for (int i = playerLines.size() - 1; i >= 0; i--) {
            PVector[] line = playerLines.get(i);
            PVector p1 = line[0];
            PVector p2 = line[1];

            float distanceToLine = distToSegment(new PVector(x, y), p1, p2);

            if (distanceToLine < App.CELLSIZE / 2) {
                PVector lineVector = PVector.sub(p2, p1).normalize();
                PVector normal = new PVector(-lineVector.y, lineVector.x);
                PVector velocity = new PVector(vx, vy);
                vx = velocity.x - 2 * PVector.dot(velocity, normal) * normal.x;
                vy = velocity.y - 2 * PVector.dot(velocity, normal) * normal.y;

                playerLines.remove(i);
                break;
            }
        }
    }

    void changeBallColorBasedOnWall(char wallType) {
        switch (wallType) {
            case '1':
                image = applet.loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball1.png");
                break;
            case '2':
                image = applet.loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball2.png");
                break;
            case '3':
                image = applet.loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball3.png");
                break;
            case '4':
                image = applet.loadImage("/Users/a/Downloads/inkball_scaffold/src/main/resources/inkball/ball4.png");
                break;
            default:
                break;
        }
    }

    float distToSegment(PVector p, PVector v, PVector w) {
        float l2 = PVector.dist(v, w) * PVector.dist(v, w);
        if (l2 == 0.0) return PVector.dist(p, v);
        float t = PVector.dot(PVector.sub(p, v), PVector.sub(w, v)) / l2;
        t = Math.max(0, Math.min(1, t));
        PVector projection = PVector.add(v, PVector.mult(PVector.sub(w, v), t));
        return PVector.dist(p, projection);
    }

    void display(PApplet applet) {
        applet.image(image, x, y);
    }

    void handleWallCollision(String[] layout) {
        int col = (int) (x / App.CELLSIZE);
        int row = (int) ((y - App.TOPBAR) / App.CELLSIZE);

        if (col < 0 || col >= layout[0].length() || row < 0 || row >= layout.length) {
            return;
        }

        char cell = layout[row].charAt(col);

        if (cell == 'X' || cell == '1' || cell == '2' || cell == '3' || cell == '4') {
            changeBallColorBasedOnWall(cell);

            float prevX = x - vx;
            float prevY = y - vy;

            int prevCol = (int) (prevX / App.CELLSIZE);
            int prevRow = (int) ((prevY - App.TOPBAR) / App.CELLSIZE);

            PVector velocity = new PVector(vx, vy);
            PVector normal = new PVector();

            if (prevCol != col) {
                if (vx > 0) {
                    normal = new PVector(-1, 0);
                } else {
                    normal = new PVector(1, 0);
                }
            }

            if (prevRow != row) {
                if (vy > 0) {
                    normal = new PVector(0, -1);
                } else {
                    normal = new PVector(0, 1);
                }
            }

            float dotProduct = velocity.dot(normal);
            PVector reflection = PVector.sub(velocity, PVector.mult(normal, 2 * dotProduct));
            vx = reflection.x;
            vy = reflection.y;
        }

        if (x <= 0 || x + App.CELLSIZE >= App.WIDTH) {
            PVector normal = new PVector(x <= 0 ? 1 : -1, 0);
            PVector velocity = new PVector(vx, vy);
            float dotProduct = velocity.dot(normal);
            PVector reflection = PVector.sub(velocity, PVector.mult(normal, 2 * dotProduct));
            vx = reflection.x;
            vy = reflection.y;
            x = (x <= 0) ? 0 : App.WIDTH - App.CELLSIZE;
        }
        if (y <= App.TOPBAR || y + App.CELLSIZE >= App.HEIGHT) {
            PVector normal = new PVector(0, y <= App.TOPBAR ? 1 : -1);
            PVector velocity = new PVector(vx, vy);
            float dotProduct = velocity.dot(normal);
            PVector reflection = PVector.sub(velocity, PVector.mult(normal, 2 * dotProduct));
            vx = reflection.x;
            vy = reflection.y;
            y = (y <= App.TOPBAR) ? App.TOPBAR : App.HEIGHT - App.CELLSIZE;
        }
    }
}
