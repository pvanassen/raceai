package nl.pvanassen.raceai;

import lombok.Getter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static java.lang.Math.*;
import static nl.pvanassen.raceai.ImageHelper.loadImage;

public class Car {

    private static final BufferedImage CAR = loadImage("car.png");

    private static final BufferedImage CRASHED = loadImage("crashed.png");

    private static final BufferedImage BEST = loadImage("winner.png");

    private static final double MAX_SPEED = 100;
    private static final double MAX_ACCELERATION = 0.1;
    private static final double MAX_DECELERATION = 0.2;
    private static final double MAX_TURN = 4;

    private final List<Line2D> checkpoints;

    private Line2D nextCheckpoint;

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

    private double distanceRight;

    private double distanceAhead;

    private double distanceLeft;

    private long start = Long.MIN_VALUE;

    @Getter
    private long lifetime;

    @Getter
    private final String id;

    private final boolean best;

    private boolean lapComplete = false;

    Car(CarType carType, Point startLocation, List<Line2D> checkpoints) {
        this.checkpoints = checkpoints;
        nextCheckpoint = checkpoints.get(0);
        x = startLocation.x;
        y = startLocation.y;
        this.id = "Car-" + carType.getId();
        this.best = carType.isBest();
    }

    public void tick(BufferedImage buffer) {
        if (start == Long.MIN_VALUE) {
            start = System.currentTimeMillis();
        }
        Graphics2D graphics = (Graphics2D)buffer.getGraphics();

        if (crashed) {
            draw(graphics, CRASHED);
            return;
        }

        if (System.currentTimeMillis() - start > 5000 && speed == 0) {
            crashed();
            draw(graphics, CRASHED);
            return;
        }

        if (System.currentTimeMillis() - start > 10000 && checkpoints.indexOf(nextCheckpoint) < 2 && !lapComplete) {
            crashed();
            draw(graphics, CRASHED);
            return;
        }

        if (System.currentTimeMillis() - start > 60000) {
            crashed();
            draw(graphics, CRASHED);
            return;
        }

        if (speed > 0) {
            if (nextCheckpoint.ptLineDist(x, y) < 10d) {
                score += 500;
                int pos = checkpoints.indexOf(nextCheckpoint) + 1;
                if (pos == checkpoints.size()) {
                    pos = 0;
                    lapComplete = true;
                }
                nextCheckpoint = checkpoints.get(pos);
            }
        }

        x += speed * sin(toRadians(90 - direction));
        y += speed * cos(toRadians(90 - direction));

        score += (speed * 100);
        if (best) {
            draw(graphics, BEST);
        }
        else {
            draw(graphics, CAR);
        }
    }

    private void draw(Graphics2D graphics, BufferedImage image) {
        AffineTransform at = new AffineTransform();
        at.translate(x, y);
        at.rotate(toRadians(direction));
        graphics.drawImage(image, at, null);

        Rectangle rectangle = new Rectangle(20, 10);
        at.translate(0, 5);
        shape = at.createTransformedShape(rectangle);

        if (Global.DEBUG) {
            graphics.setColor(Color.BLUE);
            graphics.draw(shape);
        }

        at.translate(10, 5);

        if (Global.DEBUG) {
            graphics.setColor(Color.BLUE);
            graphics.setFont(graphics.getFont().deriveFont(16f));
            graphics.drawString(id, (int) x, (int) y);
        }
    }

    public void action(Accelerate accelerate, Turn turn) {
        if (crashed) {
            return;
        }
        if (accelerate == Accelerate.ACCELERATE) {
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

    LinesOfSightWithCallback getLinesOfSight() {
        return LinesOfSightWithCallback.builder()
                .linesOfSight(LinesOfSight.builder()
                        .x((int)x)
                        .y((int)y)
                        .direction((int)direction)
                        .build())
                .linesOfSightDistances(this::recievedDistances)
                .build();
    }

    void recievedDistances(LinesOfSightDistances linesOfSightDistances) {
        distanceRight = linesOfSightDistances.getDistanceRight();
        distanceAhead = linesOfSightDistances.getDistanceAhead();
        distanceLeft = linesOfSightDistances.getDistanceLeft();
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
