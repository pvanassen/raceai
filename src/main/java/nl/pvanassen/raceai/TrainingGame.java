package nl.pvanassen.raceai;

import lombok.SneakyThrows;
import nl.pvanassen.raceai.ai.CarAI;
import nl.pvanassen.raceai.ai.Population;

import java.awt.*;

import static nl.pvanassen.raceai.Global.POPULATION_SIZE;

public class TrainingGame extends Game {

    private final DebugFrame debugFrame = new DebugFrame();

    private final Population population;

    private TrainingGame() {
        super(Modus.TRAINING);
        population = new Population(track, POPULATION_SIZE);
        if (Global.DEBUG) {
            EventQueue.invokeLater(() -> {
                debugFrame.replaceCar(population.getFirstCar());
            });
        }
        start();
    }

    @SneakyThrows
    protected void tick() {
        population.tick();
        if (population.done()) {
            population.getBest().ifPresent(CarAI::saveBrain);
            System.out.println("Done, next round!");
            track.clear();
            population.naturalSelection();
            if (Global.DEBUG) {
                EventQueue.invokeLater(() -> {
                    debugFrame.replaceCar(population.getFirstCar());
                });
            }
        }
        debugFrame.repaint();
    }

    public static void main(String[] args) {
        new TrainingGame();
    }

}
