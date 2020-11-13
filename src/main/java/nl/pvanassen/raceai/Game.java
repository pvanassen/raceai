package nl.pvanassen.raceai;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static nl.pvanassen.raceai.Global.FPS;

public abstract class Game {

    private static final int TICK_IN_NANOS = (int)((1 / (double) FPS) * 1000 * 1000 * 1000);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    protected final JFrame trackFrame = new JFrame("Track");

    protected final Track track = new Track();

    protected final Modus modus;

    protected Game(Modus modus) {
        this.modus = modus;
    }

    protected void start() {
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

            scheduler.scheduleAtFixedRate(this::tickInternal,
                    TICK_IN_NANOS,
                    TICK_IN_NANOS,
                    TimeUnit.NANOSECONDS);
        });
    }

    private void tickInternal() {
        try {
            track.tick();
            tick();
            track.paintNow(trackFrame.getGraphics());
            // trackFrame.repaint();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    protected abstract void tick();

}
