package nl.pvanassen.raceai.ai;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.pvanassen.raceai.*;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static nl.pvanassen.raceai.CarType.createBest;
import static nl.pvanassen.raceai.CarType.createNormal;

@Getter
public class CarAI {

    private static final int LAYERS = 3;

    private static final int NODES = 6;

    private static final AtomicInteger carNumber = new AtomicInteger(0);

    @Getter
    private final NeuralNet brain;

    private final Car car;

    private final Function<CarType, Car> carProducer;

    private final String id;

    private double fitness = Double.MIN_VALUE;

    @Setter
    private BiConsumer<double[], double[]> visionDecisionConsumer;

    CarAI(Function<CarType, Car> carProducer) {
        this.carProducer = carProducer;
        this.id = "CarAI-" + carNumber.getAndIncrement();
        this.car = carProducer.apply(createNormal(this.id));
        brain = new NeuralNet(5, NODES, 9, LAYERS, "net-" + car.getId());
    }

    public CarAI(Function<CarType, Car> carProducer, String id, String json) {
        this.carProducer = carProducer;
        this.id = id;
        this.car = carProducer.apply(createNormal(this.id));
        this.brain = NeuralNet.fromJson(json);
    }

    private CarAI(Function<CarType, Car> carProducer, NeuralNet brain) {
        this.carProducer = carProducer;
        this.id = "CarAI-" + carNumber.getAndIncrement();
        this.car = carProducer.apply(createNormal(this.id));
        this.brain = brain;
    }

    private CarAI(Function<CarType, Car> carProducer, NeuralNet brain, String id) {
        this.carProducer = carProducer;
        this.id = id;
        this.car = carProducer.apply(createBest(this.id));
        this.brain = brain;
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

    public CarAI crossoverAndMutate(CarAI selectParent, float mutationRate) {
        return new CarAI(carProducer, brain.crossoverAndMutate(selectParent.brain, mutationRate));
    }

    public double calculateFitness() {
        if (fitness == Double.MIN_VALUE) {
            fitness = car.getScore();
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
