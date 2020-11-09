package nl.pvanassen.raceai;

import nl.pvanassen.raceai.ai.Population;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final int FPS = 60;

    private static final int TICK_IN_NANOS = (int)((1 / (double) FPS) * 1000 * 1000 * 1000);

    public static final boolean DEBUG = true;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final JFrame trackFrame = new JFrame("Track");

    private final JFrame debugFrame = new JFrame("Debug");

    private final Track track = new Track();

    private Car manual;

    private Accelerate accelerate = Accelerate.IDLE;

    private Turn turn = Turn.STRAIGHT;

    private final boolean manualDriving = false;

    private Population population;

    private Main() {
        if (manualDriving) {
             manual = track.createCar("manual");
        }
        EventQueue.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }

            trackFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            trackFrame.add(track);
            trackFrame.pack();
            trackFrame.setLocationRelativeTo(null);
            trackFrame.setVisible(true);

            if (manualDriving) {
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
            }
            else {
                population = new Population(track, 10); //adjust size of population
            }
            if (Main.DEBUG && !manualDriving) {
                debugFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                debugFrame.setLocationRelativeTo(trackFrame);
                debugFrame.add(new DebugPanel(population.getFirstCar()));
                debugFrame.pack();
                debugFrame.setVisible(true);
            }
            scheduler.scheduleAtFixedRate(this::tick,
                    TICK_IN_NANOS,
                    TICK_IN_NANOS,
                    TimeUnit.NANOSECONDS);
        });
    }

    void tick() {
        try {
            if (manualDriving) {
                manual.action(accelerate, turn);
                accelerate = Accelerate.IDLE;
                turn = Turn.STRAIGHT;
            }
            else {
                population.tick();
            }
            if (!manualDriving && population.done()) {
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
            trackFrame.repaint();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Main();
    }

}
