package nl.pvanassen.raceai;

import lombok.NoArgsConstructor;

import java.util.concurrent.ForkJoinPool;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Global {

    public static final boolean DEBUG = true;

    public static final ForkJoinPool POOL = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);

    public static final int FPS = 60;

    public static final int POPULATION_SIZE = 50;

}
