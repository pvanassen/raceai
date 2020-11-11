package nl.pvanassen.raceai;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;
import static nl.pvanassen.raceai.ImageHelper.loadImage;

public class Track extends JPanel {

    private static final Point START_LOCATION = new Point(440, 390);

    private final BufferedImage track = loadImage("track1.png");

    private final BufferedImage mask = loadImage("track1-mask.png");

    private final List<Point> collisionSet = new LinkedList<>();

    private final List<Car> cars = new LinkedList<>();

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
        List<ForkJoinTask<List<LinesOfSight>>> tasks = Lists.partition(cars, partitionSize)
                .stream()
                .map(CollisionTask.create(mask, buffer))
                .map(Global.POOL::submit)
                .collect(Collectors.toList());

        List<LinesOfSight> linesOfSight = tasks.stream()
                .map(ForkJoinTask::join)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        new DistanceTask(buffer, collisionSet, linesOfSight).run();
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class CollisionTask implements Callable<List<LinesOfSight>> {
        private final BufferedImage mask;

        private final BufferedImage buffer;

        private final List<Car> cars;

        static Function<List<Car>, CollisionTask> create(BufferedImage mask, BufferedImage buffer) {
            return cars -> new CollisionTask(mask, buffer, cars);
        }

        @Override
        public List<LinesOfSight> call() {
            List<LinesOfSight> linesOfSights = new LinkedList<>();
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

                    linesOfSights.add(car.getLinesOfSight());
                }
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
            return linesOfSights;
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
    }

    @RequiredArgsConstructor
    private static class DistanceTask implements Runnable {
        private final BufferedImage buffer;

        private final List<Point> collisionSet;

        private final List<LinesOfSight> linesOfSights;

        @Override
        public void run() {
            double distanceLeft;
            double minDistanceLeft;
            double distanceAhead;
            double minDistanceAhead;
            double distanceRight;
            double minDistanceRight;
            for (LinesOfSight linesOfSight : linesOfSights) {
                minDistanceLeft = Double.MAX_VALUE;
                minDistanceAhead = Double.MAX_VALUE;
                minDistanceRight = Double.MAX_VALUE;
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
