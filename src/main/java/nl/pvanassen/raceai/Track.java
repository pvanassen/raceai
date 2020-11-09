package nl.pvanassen.raceai;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

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

    Track() {
        setPreferredSize(new Dimension(mask.getWidth(), mask.getHeight()));
        setDoubleBuffered(true);
    }

    public Car createCar(String id) {
        Car car = new Car(id, START_LOCATION, checkpoints);
        cars.add(car);
        return car;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D graphics2D = (Graphics2D)g;
        Dimension size = getSize();
        g.drawImage(track, 0, 0,size.width, size.height,0, 0, track.getWidth(), track.getHeight(), null);
        if (Main.DEBUG) {
            g.setColor(Color.BLUE);
            checkpoints.forEach(((Graphics2D) g)::draw);
        }

        List<Car> cars = new ArrayList<>(this.cars);
        for (Car car : cars) {
            car.draw(graphics2D);
            if (Main.DEBUG) {
                graphics2D.setColor(Color.BLUE);
                graphics2D.draw(car.getShape());
            }

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
            if (Main.DEBUG) {
                graphics2D.setColor(Color.BLUE);
                graphics2D.draw(lineOfSight);
            }
            double minDistance = Double.MAX_VALUE;
            double distance;
            for (int x = 0; x != mask.getWidth(); x++) {
                for (int y = 0; y != mask.getHeight(); y++) {
                    if (lineOfSight.intersects(x, y, 1, 1)) {
                        if (mask.getRGB(x ,y) != Color.WHITE.getRGB()) {
                            if (Main.DEBUG) {
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

    public void clear() {
        cars.clear();
    }
}
