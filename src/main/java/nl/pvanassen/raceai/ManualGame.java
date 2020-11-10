package nl.pvanassen.raceai;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ManualGame extends Game {

    private final Car manual;

    private Accelerate accelerate = Accelerate.IDLE;

    private Turn turn = Turn.STRAIGHT;

    private ManualGame() {
        super(Modus.MANUAL);
        manual = track.createCar(CarType.createNormal("manual"));

        EventQueue.invokeLater(() -> {
            trackFrame.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e) {
                    keyPressed(e);
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    keyPressed(e);
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    int keyCode = e.getKeyCode();
                    switch (keyCode) {
                        case KeyEvent.VK_UP:
                            accelerate = Accelerate.ACCELERATE;
                            break;
                        case KeyEvent.VK_DOWN:
                            accelerate = Accelerate.DECELERATE;
                            break;
                        case KeyEvent.VK_LEFT:
                            turn = Turn.LEFT;
                            break;
                        case KeyEvent.VK_RIGHT:
                            turn = Turn.RIGHT;
                            break;
                    }
                }
            });
        });
    }

    protected void tick() {
        manual.action(accelerate, turn);
        accelerate = Accelerate.IDLE;
        turn = Turn.STRAIGHT;
    }

    public static void main(String[] args) {
        new ManualGame();
    }

}
