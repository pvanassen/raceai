package nl.pvanassen.raceai;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.*;
import static lombok.AccessLevel.PRIVATE;
import static nl.pvanassen.raceai.ImageHelper.loadImage;

public class Track extends JPanel {

    private static final Point START_LOCATION = new Point(440, 390);

    private final BufferedImage track = loadImage("track1.png");

    private final BufferedImage mask = loadImage("track1-mask.png");

    private final List<Point> collisionSet = new LinkedList<>();

    private final List<Car> cars = new LinkedList<>();

    private static final Map<LinesOfSight, LinesOfSightDistances> COLLISION_MAP = new ConcurrentHashMap<>(300, 0.75f, Runtime.getRuntime().availableProcessors() * 2);

    private final List<Line2D> checkpoints = List.of(new Line2D.Float(30, 300, 80, 300),
            new Line2D.Float(300, 360, 300, 410),
            new Line2D.Float(240, 190, 240, 240),
            new Line2D.Float(380, 120, 430, 150),
            new Line2D.Float(605, 65, 580, 120),
            new Line2D.Float(700, 200, 760, 225),
            new Line2D.Float(600, 180, 575, 225),
            new Line2D.Float(600, 270, 575, 320));

    private final BufferedImage buffer;

    Track() {
        setPreferredSize(new Dimension(mask.getWidth(), mask.getHeight()));
        setDoubleBuffered(true);
        buffer = new BufferedImage(mask.getWidth(), mask.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x != mask.getWidth(); x++) {
            for (int y = 0; y != mask.getHeight(); y++) {
                if (mask.getRGB(x ,y) == Color.WHITE.getRGB()) {
                    continue;
                }
                collisionSet.add(new Point(x, y));
            }
        }

    }

    public Car createCar(CarType id) {
        Car car = new Car(id, START_LOCATION, checkpoints);
        cars.add(car);
        return car;
    }

    @SneakyThrows
    public void tick() {
        Graphics2D graphics2D = (Graphics2D)buffer.getGraphics();
        Dimension size = getSize();
        graphics2D.drawImage(track, 0, 0,size.width, size.height,0, 0, track.getWidth(), track.getHeight(), null);
        if (Global.DEBUG) {
            graphics2D.setColor(Color.BLUE);
            checkpoints.forEach(graphics2D::draw);
        }

        int partitionSize = (int)Math.ceil(cars.size() / (float)Runtime.getRuntime().availableProcessors());
        List<ForkJoinTask<?>> tasks = Lists.partition(cars, partitionSize)
                .stream()
                .map(CollisionTask.create(mask, buffer, collisionSet))
                .map(Global.POOL::submit)
                .collect(Collectors.toList());

        for (ForkJoinTask<?> task : tasks) {
            task.join();
        }

    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class CollisionTask implements Runnable {
        private final BufferedImage mask;

        private final BufferedImage buffer;

        private final List<Point> collisionSet;

        private final Map<LinesOfSight, LinesOfSightDistances> collisionMap;

        private final List<Car> cars;

        static Function<List<Car>, CollisionTask> create(
                BufferedImage mask, BufferedImage buffer, List<Point> collisionSet) {
            return cars -> new CollisionTask(mask, buffer, collisionSet, Track.COLLISION_MAP, cars);
        }

        @Override
        public void run() {
            try {
                for (Car car : cars) {
                    car.tick(buffer);

                    if (car.isCrashed()) {
                        continue;
                    }

                    // Collision detection
                    if (doCollisionDetection(car)) {
                        continue;
                    }

//                    calculateDistances(car.getLinesOfSight());

                    LinesOfSight linesOfSight = car.getLinesOfSight();
                    LinesOfSightDistances linesOfSightDistances = collisionMap.computeIfAbsent(linesOfSight, this::calculateDistancesAlt1);

                    linesOfSight.getLinesOfSightDistances().accept(linesOfSightDistances);
                }
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

        private boolean doCollisionDetection(Car car) {
            for (int x = (int)car.getShape().getBounds().getX(); x != (int)car.getShape().getBounds().getX() + car.getShape().getBounds().getWidth(); x++) {
                for (int y = (int)car.getShape().getBounds().getY(); y != (int)car.getShape().getBounds().getY() + car.getShape().getBounds().getHeight(); y++) {
                    if (car.getShape().contains(x, y)) {
                        if (mask.getRGB(x, y) != Color.WHITE.getRGB()) {
                            car.crashed();
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private LinesOfSightDistances calculateDistancesAlt1(LinesOfSight linesOfSight) {
            double x = linesOfSight.getX();
            double y = linesOfSight.getY();
            double direction = linesOfSight.getDirection();
            AffineTransform at = new AffineTransform();
            at.translate(x, y);
            at.rotate(toRadians(direction));
            at.translate(10, 10);
            Point2D pt1 = at.transform(new Point2D.Double(0, 0), null);
            double startX = pt1.getX();
            double startY = pt1.getY();

            double directionXAhead = cos(toRadians(direction));
            double directionYAhead = sin(toRadians(direction));

            double directionXLeft = cos(toRadians(direction + 45));
            double directionYLeft = sin(toRadians(direction + 45));

            double directionXRight = cos(toRadians(direction - 45));
            double directionYRight = sin(toRadians(direction - 45));

            int distanceLeft = Integer.MAX_VALUE;
            int distanceAhead = Integer.MAX_VALUE;
            int distanceRight = Integer.MAX_VALUE;

            for (int i=0; i!= 1000; i++) {
                if (distanceLeft == Integer.MAX_VALUE) {
                    distanceLeft = getDistance(startX, startY, directionXLeft, directionYLeft, i);
                }
                if (distanceAhead == Integer.MAX_VALUE) {
                    distanceAhead = getDistance(startX, startY, directionXAhead, directionYAhead, i);
                }
                if (distanceRight == Integer.MAX_VALUE) {
                    distanceRight = getDistance(startX, startY, directionXRight, directionYRight, i);
                }
            }
            return LinesOfSightDistances.builder()
                    .distanceAhead(distanceAhead)
                    .distanceLeft(distanceLeft)
                    .distanceRight(distanceRight)
                    .build();
        }

        private int getDistance(double startX, double startY, double directionX, double directionY, int i) {
            int endX = (int) (startX + (directionX * i));
            int endY = (int) (startY + (directionY * i));

            if ((endX < 0 || endX > mask.getWidth()) || (endY < 0 || endY > mask.getHeight())) {
                System.out.println("StartX: " + startX + ", startY: " + startY + ", directionX: " + directionX + ", directionY: " + directionY + ", i: " + i);
                return Integer.MAX_VALUE;
            }

            if (mask.getRGB(endX, endY) != Color.WHITE.getRGB()) {
                if (Global.DEBUG) {
                    buffer.setRGB(endX, endY, Color.RED.getRGB());
                }
                return i;
            }

            if (Global.DEBUG) {
                buffer.setRGB(endX, endY, Color.GREEN.getRGB());
            }
            return Integer.MAX_VALUE;
        }

        private void calculateDistances(LinesOfSight linesOfSight) {
            double distanceLeft;
            double minDistanceLeft = Double.MAX_VALUE;
            double distanceAhead;
            double minDistanceAhead = Double.MAX_VALUE;
            double distanceRight;
            double minDistanceRight = Double.MAX_VALUE;
            if (Global.DEBUG) {
                Graphics2D graphics2D = (Graphics2D)buffer.getGraphics();
                graphics2D.setColor(Color.BLUE);
                graphics2D.draw(linesOfSight.getLineOfSightLeft());
                graphics2D.draw(linesOfSight.getLineOfSightAhead());
                graphics2D.draw(linesOfSight.getLineOfSightRight());
            }
            for (Point point : collisionSet) {
                if (linesOfSight.getLineOfSightLeft().intersects(point.x, point.y, 1, 1)) {
                    if (Global.DEBUG) {
                        buffer.setRGB(point.x, point.y, Color.RED.getRGB());
                    }
                    distanceLeft = linesOfSight.getLineOfSightLeft().getP1().distance(point.x, point.y);
                    if (distanceLeft < minDistanceLeft) {
                        minDistanceLeft = distanceLeft;
                    }
                }
                if (linesOfSight.getLineOfSightAhead().intersects(point.x, point.y, 1, 1)) {
                    if (Global.DEBUG) {
                        buffer.setRGB(point.x, point.y, Color.RED.getRGB());
                    }
                    distanceAhead = linesOfSight.getLineOfSightAhead().getP1().distance(point.x, point.y);
                    if (distanceAhead < minDistanceAhead) {
                        minDistanceAhead = distanceAhead;
                    }
                }
                if (linesOfSight.getLineOfSightRight().intersects(point.x, point.y, 1, 1)) {
                    if (Global.DEBUG) {
                        buffer.setRGB(point.x, point.y, Color.RED.getRGB());
                    }
                    distanceRight = linesOfSight.getLineOfSightRight().getP1().distance(point.x, point.y);
                    if (distanceRight < minDistanceRight) {
                        minDistanceRight = distanceRight;
                    }
                }
            }
            linesOfSight.getLinesOfSightDistances().accept(LinesOfSightDistances.builder()
                    .distanceAhead(minDistanceAhead)
                    .distanceLeft(minDistanceLeft)
                    .distanceRight(minDistanceRight)
                    .build());
        }
    }

    void paintNow(Graphics g) {
        g.drawImage(buffer, 0, 0, buffer.getWidth(), buffer.getHeight(), null);
        Graphics2D graphics = (Graphics2D) buffer.getGraphics();
        graphics.setBackground(new Color(0, 0, 0, 0));
        graphics.clearRect(0,0, buffer.getWidth(), buffer.getHeight());
    }

    public void clear() {
        cars.clear();
    }
}
