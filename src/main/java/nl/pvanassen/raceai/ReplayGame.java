package nl.pvanassen.raceai;

import lombok.SneakyThrows;
import nl.pvanassen.raceai.ai.CarAI;
import nl.pvanassen.raceai.ai.Population;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.stream.Collectors;

public class ReplayGame extends Game {

    private final JFrame debugFrame = new JFrame("Debug");

    private CarAI carAI;

    @SneakyThrows
    private ReplayGame(String file) {
        super(Modus.TRAINING);
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(file)))) {
            carAI = new CarAI(track::createCar, "replay", reader.lines().collect(Collectors.joining()));
        }

        EventQueue.invokeLater(() -> {
            if (Main.DEBUG) {
                debugFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                debugFrame.setLocationRelativeTo(trackFrame);
                debugFrame.add(new DebugPanel(carAI));
                debugFrame.pack();
                debugFrame.setVisible(true);
            }
        });
    }

    @SneakyThrows
    protected void tick() {
        carAI.calculate();
        if (!carAI.isAlive()) {
            Thread.sleep(2000);
            track.clear();
            carAI = carAI.cloneForReplay();
            if (Main.DEBUG) {
                EventQueue.invokeLater(() -> {
                    for (int i = 0; i < debugFrame.getComponents().length; i++) {
                        if (debugFrame.getComponent(i) instanceof DebugPanel) {
                            debugFrame.remove(i);
                            break;
                        }
                    }
                    debugFrame.add(new DebugPanel(carAI));
                });
            }
        }
    }

    public static void main(String[] args) {
        new ReplayGame("CarAI-34-1604955841841.json");
    }

}
