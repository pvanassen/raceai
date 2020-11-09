package nl.pvanassen.raceai.ai;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.pvanassen.raceai.Accelerate;
import nl.pvanassen.raceai.Car;
import nl.pvanassen.raceai.CarMetrics;
import nl.pvanassen.raceai.Turn;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Getter
public class CarAI {

    private static final AtomicInteger carNumber = new AtomicInteger(0);

    @Getter
    private final NeuralNet brain;

    private final Car car;

    private final Function<String, Car> carProducer;

    private final String id;

    private double fitness = Double.MIN_VALUE;

    @Setter
    private BiConsumer<double[], double[]> visionDecisionConsumer;

    CarAI(Function<String, Car> carProducer) {
        this.carProducer = carProducer;
        this.id = "CarAI-" + carNumber.getAndIncrement();
        this.car = carProducer.apply(this.id);
        brain = new NeuralNet(5, 9, 9, 2, "net-" + car.getId());
        System.out.println("CarAI: " + id + ", car: " + car.getId() + ", brain: " + brain.getId());
    }

    public CarAI(Function<String, Car> carProducer, String id, String json) {
        this.carProducer = carProducer;
        this.id = id;
        this.car = carProducer.apply(this.id);
        this.brain = NeuralNet.fromJson(json);
    }

    private CarAI(Function<String, Car> carProducer, NeuralNet brain) {
        this.carProducer = carProducer;
        this.id = "CarAI-" + carNumber.getAndIncrement();
        this.car = carProducer.apply(this.id);
        this.brain = brain;
    }

    private CarAI(Function<String, Car> carProducer, NeuralNet brain, String id) {
        this.carProducer = carProducer;
        this.car = carProducer.apply(id);
        this.brain = brain;
        this.id = id;
    }

    public boolean isAlive() {
        return !car.isCrashed();
    }

    public void calculate() {
        CarMetrics carMetrics = car.getCarMetrics();
        double[] result = brain.output(carMetrics.getInput());

        if (visionDecisionConsumer != null) {
            visionDecisionConsumer.accept(carMetrics.getInput(), result);
        }

        double max = 0;
        int idx = -1;
        for (int i = 0; i < result.length; i++) {
            if (result[i] > max) {
                max = result[i];
                idx = i;
            }
        }

        Accelerate accelerate;
        Turn turn;
        if (idx < 3) {
            accelerate = Accelerate.ACCELERATE;
        }
        else if (idx < 6) {
            accelerate = Accelerate.IDLE;
        }
        else {
            accelerate = Accelerate.DECELERATE;
        }
        if (idx == 0 || idx == 3 || idx == 6) {
            turn = Turn.LEFT;
        }
        else if (idx == 1 || idx == 4 || idx == 7) {
            turn = Turn.STRAIGHT;
        }
        else {
            turn = Turn.RIGHT;
        }

        car.action(accelerate, turn);
    }

    public double getScore() {
        return car.getScore();
    }

    public CarAI cloneForReplay() {
        return new CarAI(carProducer, brain.copy(), id);
    }

    public CarAI crossoverAndMutate(CarAI selectParent) {
        return new CarAI(carProducer, brain.crossoverAndMutate(selectParent.brain, 0.01f));
    }

    public double calculateFitness() {
        if (fitness == Double.MIN_VALUE) {
            fitness = car.getScore();
            System.out.println("Car " + id + ", score: " + car.getScore());
        }
        return fitness;
    }

    @SneakyThrows
    public void saveBrain() {
        File file = new File(id + "-" + System.currentTimeMillis() + ".json");
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(brain.toJson());
            fileWriter.flush();
        }
    }
}
