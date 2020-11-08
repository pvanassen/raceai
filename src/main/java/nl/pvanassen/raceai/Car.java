package nl.pvanassen.raceai;

import lombok.Getter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import static java.lang.Math.*;
import java.util.List;
import static nl.pvanassen.raceai.ImageHelper.loadImage;

public class Car implements Drawable {

    private static final BufferedImage CAR = loadImage("car.png");
    private static final BufferedImage CRASHED = loadImage("crashed.png");
    private static final BufferedImage WINNER = loadImage("winner.png");
    private static final double MAX_SPEED = 100;
    private static final double MAX_ACCELERATION = 0.1;
    private static final double MAX_DECELERATION = 0.2;
    private static final double MAX_TURN = 4;
    private static final int DEFAULT_LINE_OF_SIGHT = 1000;

    private final Point startLocation;

    private final List<Line2D> checkpoints;

    @Getter
    private boolean crashed = false;

    @Getter
    private double x;
    @Getter
    private double y;

    private double direction = 180;
    private double speed = 0;

    @Getter
    private Shape shape;

    @Getter
    private double score;

    private boolean started;

    private Line2D.Double lineOfSightRight;

    private Line2D.Double lineOfSightAhead;

    private Line2D.Double lineOfSightLeft;

    private double distanceRight;

    private double distanceAhead;

    private double distanceLeft;

    private long start = System.currentTimeMillis();

    @Getter
    private long lifetime;

    @Getter
    private final String id;

    Car(String id, Point startLocation, List<Line2D> checkpoints) {
        this.startLocation = startLocation;
        this.checkpoints = checkpoints;

        x = startLocation.x;
        y = startLocation.y;
        this.id = "Car-" + id;
    }

    @Override
    public void draw(Graphics2D graphics) {
        if (crashed) {
            draw(graphics, CRASHED);
            return;
        }

        if (System.currentTimeMillis() - start > 1000 && score == 0) {
            System.out.println("Timed out, no score: " + id);
            crashed();
            draw(graphics, CRASHED);
            return;
        }

        if (System.currentTimeMillis() - start > 2000 && speed == 0) {
            System.out.println("Timed out, no movement: " + id);
            crashed();
            draw(graphics, CRASHED);
            return;
        }

        if (System.currentTimeMillis() - start > 60000) {
            System.out.println("Driven long enough: " + id);
            crashed();
            draw(graphics, CRASHED);
            return;
        }

        if (speed > 0) {
            for (Line2D checkpoint : checkpoints) {
                if (checkpoint.ptLineDist(x, y) < 0.1d) {
                    System.out.println("Car passing checkpoint: " + id);
                    score += 500;
                }
            }
        }

        x += speed * sin(toRadians(90 - direction));
        y += speed * cos(toRadians(90 - direction));

        score += speed;

        draw(graphics, CAR);
    }

    private void draw(Graphics2D graphics, BufferedImage image) {
        AffineTransform at = new AffineTransform();
        at.translate(x + 10, y + 10);
        at.rotate(toRadians(direction));
        graphics.drawImage(image, at, null);

        Rectangle rectangle = new Rectangle(20, 10);
        at.translate(0, 5);
        shape = at.createTransformedShape(rectangle);

        lineOfSightRight = getLine(direction + 45);
        lineOfSightAhead = getLine(direction);
        lineOfSightLeft = getLine(direction - 45);

        if (Main.DEBUG) {
            graphics.setColor(Color.BLUE);
            graphics.setFont(new Font("TimesRoman", Font.BOLD, 12));
            graphics.drawString(id, (int) x, (int) y);
        }
    }

    private Line2D.Double getLine(double direction) {
        double endX = x + cos(toRadians(direction)) * DEFAULT_LINE_OF_SIGHT;
        double endY = y + sin(toRadians(direction)) * DEFAULT_LINE_OF_SIGHT;
        return new Line2D.Double(x, y, endX, endY);
    }

    public void action(Accelerate accelerate, Turn turn) {
        if (crashed) {
            return;
        }
        if (accelerate == Accelerate.ACCELERATE) {
            started = true;
            speed = Math.min(MAX_SPEED, speed + MAX_ACCELERATION);
        }
        if (accelerate == Accelerate.DECELERATE) {
            speed = Math.max(0, speed - MAX_DECELERATION);
        }
        if (speed == 0) {
            return;
        }
        double maxTurn;
        if (speed < 0.3) {
            maxTurn = 1;
        }
        else {
            maxTurn = MAX_TURN;
        }
        if (turn == Turn.LEFT) {
            direction -= maxTurn;
        }
        if (turn == Turn.RIGHT) {
            direction += maxTurn;
        }
        if (direction < 0) {
            direction += 360;
        }
        if (direction > 360) {
            direction -= 360;
        }
    }

    public void crashed() {
        lifetime = System.currentTimeMillis() - start;
        crashed = true;
    }

    void calculateDistances(Function<Line2D.Double, Double> distanceCalculation) {
        distanceRight = distanceCalculation.apply(lineOfSightRight);
        distanceAhead = distanceCalculation.apply(lineOfSightAhead);
        distanceLeft = distanceCalculation.apply(lineOfSightLeft);
    }

    public CarMetrics getCarMetrics() {
        return CarMetrics.builder()
                .direction(direction)
                .speed(speed)
                .distanceLeft(distanceLeft)
                .distanceAhead(distanceAhead)
                .distanceRight(distanceRight)
                .build();
    }
}
