package nl.pvanassen.raceai;

import nl.pvanassen.raceai.ai.CarAI;

import javax.swing.*;

public class DebugFrame extends JFrame {

    DebugFrame() {
        super("Debug");
        setVisible(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    void replaceCar(CarAI newCar) {
        for (int i = 0; i < getComponents().length; i++) {
            if (getComponent(i) instanceof DebugPanel) {
                remove(i);
                break;
            }
        }
        add(new DebugPanel(newCar));
        pack();
        if (!isVisible()) {
            setVisible(true);
        }
    }
}
