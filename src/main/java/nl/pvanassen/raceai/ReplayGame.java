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

    private final DebugFrame debugFrame = new DebugFrame();

    private CarAI carAI;

    @SneakyThrows
    private ReplayGame(String file) {
        super(Modus.TRAINING);
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(file)))) {
            carAI = new CarAI(track::createCar, "replay", reader.lines().collect(Collectors.joining()));
        }

        EventQueue.invokeLater(() -> {
            if (Global.DEBUG) {
                debugFrame.replaceCar(carAI);
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
            if (Global.DEBUG) {
                EventQueue.invokeLater(() -> {
                    debugFrame.replaceCar(carAI);
                });
            }
        }
    }

    public static void main(String[] args) {
        new ReplayGame("CarAI-34-1604955841841.json");
    }

}
