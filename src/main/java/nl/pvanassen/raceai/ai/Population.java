package nl.pvanassen.raceai.ai;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import nl.pvanassen.raceai.Global;
import nl.pvanassen.raceai.Track;
import org.checkerframework.common.value.qual.IntRange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class Population {

    private final List<CarAI> cars;

    private CarAI bestCar;

    private int bestScore = 0;
    private int gen = 0;

    private double bestFitness = 0;

    private int roundsWithNoFitnessIncrease = 0;

    public Population(Track track, int size) {
        cars = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            cars.add(new CarAI(track::createCar));
        }
    }

    public boolean done() {
        // Skip best car
        for (CarAI car : cars.subList(1, cars.size())) {
            if (car.isAlive()) {
                return false;
            }
        }
        return true;
    }

    @SneakyThrows
    public void tick() {
        List<CarAI> aliveCars =  cars.stream()
                .filter(CarAI::isAlive)
                .collect(toList());
        if (cars.isEmpty()) {
            return;
        }

        int partitionSize = Math.max(1, (int)Math.ceil(aliveCars.size() / (float)Runtime.getRuntime().availableProcessors()));
        List<ForkJoinTask<?>> tasks = Lists.partition(aliveCars, partitionSize)
                .stream()
                .map(Task::new)
                .map(Global.POOL::submit)
                .collect(toList());

        for (ForkJoinTask<?> task : tasks) {
            task.join();
        }
    }

    public CarAI getRandomCar() {
        return cars.get(ThreadLocalRandom.current().nextInt(cars.size())).cloneForReplay();
    }

    static class Task implements Runnable {
        private final List<CarAI> cars;

        Task(List<CarAI> cars) {
            this.cars = cars;
        }

        @Override
        public void run() {
            try {
                cars.forEach(CarAI::calculate);
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    public Optional<CarAI> getBest() {  //set the best snake of the generation
        double max = 0;
        int maxIndex = 0;
        for (int i = 0; i < cars.size(); i++) {
            double fitness = cars.get(i).calculateFitness();
            if (fitness > max) {
                max = fitness;
                maxIndex = i;
            }
        }
        System.out.println("Best score global: " + bestFitness);
        System.out.println("Best score population: " + max);
        if (max > bestFitness) {
            bestFitness = max;
            bestScore = (int)cars.get(maxIndex).getScore();
            bestCar = cars.get(maxIndex);
            roundsWithNoFitnessIncrease = 0;
            System.out.println("Fitness increase!");
            System.out.println("New best car: " + bestCar.getId());
        }
        else {
            System.out.println("No fitness increase");
            roundsWithNoFitnessIncrease++;
        }
        return Optional.ofNullable(bestCar)
                .map(CarAI::cloneForReplay);
    }

    private CarAI selectFromTop10() {
        return cars.stream()
                .sorted(Comparator.comparing(CarAI::calculateFitness))
                .limit((int) (cars.size() * 0.1f))
                .min((o1, o2) -> ThreadLocalRandom.current().nextInt(3) - 1)
                .orElseThrow();
    }

    private CarAI selectRandomParent(List<CarAI> cars) {  //selects a random number in range of the fitnesssum and if a car falls in that range then select it
        double rand = ThreadLocalRandom.current().nextDouble() * calculateFitnessSum();
        float summation = 0;
        for (CarAI car : cars) {
            summation += car.calculateFitness();
            if (summation > rand) {
                return car;
            }
        }
        return cars.get(0);
    }

    @SneakyThrows
    public void naturalSelection() {
        List<CarAI> top10Cars = cars.stream()
                .sorted(Comparator.comparing(CarAI::calculateFitness))
                .limit((int) (cars.size() * 0.1f))
                .collect(toList());;

        List<CarAI> newCars = new ArrayList<>(cars.size());

        newCars.add(getBest()
                .orElseGet(this::getRandomCar));

        float mutationRate;
        if (roundsWithNoFitnessIncrease > 10) {
            mutationRate = 0.5f;
        }
        else if (roundsWithNoFitnessIncrease > 5) {
            mutationRate = 0.1f;
        }
        else {
            mutationRate = 0.01f;
        }

        ForkJoinTask<List<CarAI>> forkJoinTask = Global.POOL.submit(() -> IntStream.rangeClosed(1, cars.size() - 1)
                .parallel()
                .mapToObj(it -> selectRandomParent(cars).crossoverAndMutate(selectRandomParent(cars), mutationRate))
                .collect(toList()));

        newCars.addAll(forkJoinTask.get());

//        for (int i = 1; i < cars.size(); i++) {
//            CarAI child = selectRandomParent(cars).crossoverAndMutate(selectRandomParent(cars), mutationRate);
//            newCars.add(child);
//        }

        cars.clear();
        cars.addAll(newCars);
        gen += 1;
    }

//    public void mutate() {
//        for (Snake snake : snakes) {
//            snake.mutate();
//        }
//    }

    public double calculateFitnessSum() {  //calculate the sum of all the snakes fitnesses
        double fitnessSum = 0;
        for (CarAI car : cars) {
            fitnessSum += car.calculateFitness();
        }
        return fitnessSum;
    }

    public CarAI getFirstCar() {
        return cars.get(0);
    }
}
