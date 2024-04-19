package application;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class main extends Application {

    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int TARGET_COUNT = 9;
    private static final double TARGET_WIDTH = 20;
    private static final double TARGET_HEIGHT = 65;
    private static final double CANNON_WIDTH = 30;
    private static final double CANNON_HEIGHT = 20;
    private static final double BARREL_LENGTH = 40;
    private static final double CANNONBALL_RADIUS = 5;
    private static final double MIN_SEPARATION_DISTANCE = 80; // Increased separation distance
    private static final double MAX_SPEED = 2.0;

    private static final int GAME_DURATION_SECONDS = 10;

    private Pane root;
    private Rectangle cannonBody;
    private Polygon cannonBarrel;

    @Override
    public void start(Stage primaryStage) {
        root = new Pane();
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        // Create Blocker at the center of the stage
        Rectangle blocker = createBouncingRectangle((WIDTH - 20) / 2, (HEIGHT - 90) / 2, 20, 90, Color.BLUE, 2.0);
        root.getChildren().add(blocker);

        // Create Cannon at the extreme left
        cannonBody = new Rectangle(0, HEIGHT / 2 - CANNON_HEIGHT / 2, CANNON_WIDTH, CANNON_HEIGHT);
        cannonBarrel = createCannonBarrel(0 + CANNON_WIDTH, HEIGHT / 2, BARREL_LENGTH);
        root.getChildren().addAll(cannonBody, cannonBarrel);

        // Create Targets scattered on the right side of the blocker without colliding
        for (int i = 0; i < TARGET_COUNT; i++) {
            double targetX;
            double targetY;

            // Ensure targets don't collide with each other and stay within the stage boundaries
            do {
                targetX = (WIDTH - 20) / 2 + 30 + Math.random() * (WIDTH - (WIDTH - 20) / 2 - 40);
                targetY = Math.random() * (HEIGHT - 40) + 20;
            } while (collidesWithOtherTargets(root, targetX, targetY, MIN_SEPARATION_DISTANCE) || isOutsideStageBounds(targetX, targetY));

            Rectangle target = createBouncingRectangle(targetX, targetY, TARGET_WIDTH, TARGET_HEIGHT, Color.RED, 0.5 + Math.random() * MAX_SPEED);
            root.getChildren().add(target);
        }

        // Cannonball firing mechanism
        scene.setOnMouseClicked(this::handleMouseClick);

        // 10-second timer
        Timeline gameTimer = new Timeline(
                new KeyFrame(Duration.seconds(GAME_DURATION_SECONDS), e -> {
                    if (!allTargetsDestroyed(root)) {
                        showGameOverAlert(primaryStage, "Time's up! You didn't destroy all targets.");
                    }
                })
        );
        gameTimer.setCycleCount(1); // Run once
        gameTimer.play();
        scene.setFill(Color.BLUEVIOLET);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Cannon Game");
        primaryStage.show();
    }

    private void handleMouseClick(MouseEvent event) {
        double mouseX = event.getSceneX();
        double mouseY = event.getSceneY();

        double angle = calculateAngle(cannonBody.getX() + CANNON_WIDTH / 2, cannonBody.getY() + CANNON_HEIGHT / 2, mouseX, mouseY);
        rotateCannon(angle);

        double cannonballX = cannonBody.getX() + CANNON_WIDTH;
        double cannonballY = cannonBody.getY() + CANNON_HEIGHT / 2;
        Circle cannonball = createCannonball(cannonballX, cannonballY, CANNONBALL_RADIUS);
        root.getChildren().add(cannonball);

        Timeline cannonballTimeline = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    double deltaX = 5 * Math.cos(Math.toRadians(angle));
                    double deltaY = 5 * Math.sin(Math.toRadians(angle));

                    cannonball.setCenterX(cannonball.getCenterX() + deltaX);
                    cannonball.setCenterY(cannonball.getCenterY() + deltaY);

                    if (checkCannonballCollision(cannonball, root)) {
                        root.getChildren().remove(cannonball);
                    }
                })
        );
        cannonballTimeline.setCycleCount(Animation.INDEFINITE);
        cannonballTimeline.play();
    }

    private void rotateCannon(double angle) {
        cannonBody.setRotate(angle);
        cannonBarrel.setRotate(angle);
    }

    private double calculateAngle(double startX, double startY, double endX, double endY) {
        return Math.toDegrees(Math.atan2(endY - startY, endX - startX));
    }

    private Circle createCannonball(double x, double y, double radius) {
        Circle cannonball = new Circle(x, y, radius, Color.BLACK);
        return cannonball;
    }

    private boolean checkCannonballCollision(Circle cannonball, Pane root) {
        for (javafx.scene.Node node : root.getChildren()) {
            if (node instanceof Rectangle && node != null) {
                Rectangle target = (Rectangle) node;
                if (cannonball.getBoundsInParent().intersects(target.getBoundsInParent())) {
                    root.getChildren().remove(target);
                    return true; // Collision detected
                }
            }
        }
        return false; // No collision
    }

    private Polygon createCannonBarrel(double startX, double startY, double length) {
        Polygon triangle = new Polygon(
                startX, startY - 5,
                startX + length, startY - 5,
                startX + length + 10, startY,
                startX + length, startY + 5,
                startX, startY + 5
        );
        triangle.setFill(Color.GRAY);
        return triangle;
    }

    private Rectangle createBouncingRectangle(double x, double y, double width, double height, Color color, double speed) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setFill(color);
        rectangle.setStroke(Color.BLACK);

        final double[] direction = {-1}; // -1 for upward movement, 1 for downward movement
        Timeline bounceTimeline = new Timeline(
                new KeyFrame(Duration.millis(16), event -> {
                    double newY = rectangle.getY() + speed * direction[0];

                    if (newY < 0 || newY + height > HEIGHT) {
                        // Reverse the direction when the rectangle hits the top or bottom
                        direction[0] *= -1;
                    } else {
                        rectangle.setY(newY);
                    }
                })
        );
        bounceTimeline.setCycleCount(Animation.INDEFINITE);
        bounceTimeline.play();

        return rectangle;
    }

    private boolean collidesWithOtherTargets(Pane root, double x, double y, double separationDistance) {
        for (javafx.scene.Node node : root.getChildren()) {
            if (node instanceof Rectangle && node != null) {
                Rectangle target = (Rectangle) node;
                if (x < target.getX() + target.getWidth() + separationDistance &&
                        x + TARGET_WIDTH + separationDistance > target.getX() &&
                        y < target.getY() + target.getHeight() + separationDistance &&
                        y + TARGET_HEIGHT + separationDistance > target.getY()) {
                    return true; // Collision detected
                }
            }
        }
        return false; // No collision
    }

    private boolean isOutsideStageBounds(double x, double y) {
        return x < 0 || y < 0 || x + TARGET_WIDTH > WIDTH || y + TARGET_HEIGHT > HEIGHT;
    }

    private boolean allTargetsDestroyed(Pane root) {
        int targetCount = 0;
        for (javafx.scene.Node node : root.getChildren()) {
            if (node instanceof Rectangle && node != null && ((Rectangle) node).getFill() == Color.RED) {
                targetCount++;
            }
        }
        return targetCount == 0;
    }

    private void showGameOverAlert(Stage primaryStage, String message) {
        // You can customize this method to show a game over alert or perform any other end-of-game actions
        System.out.println("Game Over: " + message);
        primaryStage.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}