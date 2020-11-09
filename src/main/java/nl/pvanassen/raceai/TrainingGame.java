package nl.pvanassen.raceai;

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

    private final JFrame debugFrame = new JFrame("Debug");

    private final Population population;

    private TrainingGame() {
        super(Modus.TRAINING);
        population = new Population(track, 10);
        EventQueue.invokeLater(() -> {
            if (Main.DEBUG) {
                debugFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                debugFrame.setLocationRelativeTo(trackFrame);
                debugFrame.add(new DebugPanel(population.getFirstCar()));
                debugFrame.pack();
                debugFrame.setVisible(true);
            }
        });
    }

    protected void tick() {
        population.tick();
        if (population.done()) {
            population.getBest().ifPresent(CarAI::saveBrain);
            System.out.println("Done, next round!");
            track.clear();
            population.naturalSelection();
            if (Main.DEBUG) {
                EventQueue.invokeLater(() -> {
                    for (int i = 0; i < debugFrame.getComponents().length; i++) {
                        if (debugFrame.getComponent(i) instanceof DebugPanel) {
                            debugFrame.remove(i);
                            break;
                        }
                    }
                    debugFrame.add(new DebugPanel(population.getFirstCar()));
                });
            }
        }
    }

    public static void main(String[] args) {
        new TrainingGame();
    }

}
