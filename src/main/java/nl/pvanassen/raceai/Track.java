package nl.pvanassen.raceai;

import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import nl.pvanassen.raceai.ai.Population;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;
import static nl.pvanassen.raceai.ImageHelper.loadImage;

public class Track extends JPanel {

    private static final Point START_LOCATION = new Point(440, 390);

    private final BufferedImage track = loadImage("track1.png");

    private final BufferedImage mask = loadImage("track1-mask.png");

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
                .map(Task.create(mask, graphics2D, buffer))
                .map(Global.POOL::submit)
                .collect(Collectors.toList());

        for (ForkJoinTask<?> task : tasks) {
            task.join();
        }
    }

    @RequiredArgsConstructor(access = PRIVATE)
    private static class Task implements Runnable {
        private final BufferedImage mask;

        private final Graphics2D graphics2D;

        private final BufferedImage buffer;

        private final List<Car> cars;

        static Function<List<Car>, Task> create(BufferedImage mask, Graphics2D graphics2D, BufferedImage buffer) {
            return cars -> new Task(mask, graphics2D, buffer, cars);
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

                    car.calculateDistances(calculateDistance(graphics2D, mask));
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

        private Function<Line2D.Double, Double> calculateDistance(Graphics2D graphics2D, BufferedImage mask) {
            return lineOfSight -> {
                if (Global.DEBUG) {
                    graphics2D.setColor(Color.BLUE);
                    graphics2D.draw(lineOfSight);
                }
                double minDistance = Double.MAX_VALUE;
                double distance;
                for (int x = 0; x != mask.getWidth(); x++) {
                    for (int y = 0; y != mask.getHeight(); y++) {
                        if (lineOfSight.intersects(x, y, 1, 1)) {
                            if (mask.getRGB(x ,y) != Color.WHITE.getRGB()) {
                                if (Global.DEBUG) {
                                    graphics2D.setColor(Color.RED);
                                    graphics2D.draw(new Rectangle(x, y, 1, 1));
                                }
                                distance = lineOfSight.getP1().distance(x, y);
                                if (distance < minDistance) {
                                    minDistance = distance;
                                }
                            }
                        }
                    }
                }
                return minDistance;
            };
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
