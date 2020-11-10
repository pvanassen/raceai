package nl.pvanassen.raceai;

import lombok.SneakyThrows;
import nl.pvanassen.raceai.ai.CarAI;
import nl.pvanassen.raceai.ai.Population;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TrainingGame extends Game {

    private static final int POPULATION_SIZE = 50;

    private final DebugFrame debugFrame = new DebugFrame();

    private final Population population;

    private TrainingGame() {
        super(Modus.TRAINING);
        population = new Population(track, POPULATION_SIZE);
        EventQueue.invokeLater(() -> {
            if (Global.DEBUG) {
                debugFrame.replaceCar(population.getFirstCar());
            }
        });
    }

    @SneakyThrows
    protected void tick() {
        population.tick();
        if (population.done()) {
            population.getBest().ifPresent(CarAI::saveBrain);
            Thread.sleep(1000);
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
