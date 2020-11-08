package nl.pvanassen.raceai.ai;

import com.google.common.collect.Lists;
import nl.pvanassen.raceai.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;

public class Population {

    private final ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() * 2);

    private final List<CarAI> cars;

    private CarAI bestCar;

    private int bestScore = 0;
    private int gen = 0;

    private double bestFitness = 0;

    public Population(Track track, int size) {
        cars = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            cars.add(new CarAI(track::createCar));
        }
    }

    public boolean done() {  //check if all the snakes in the population are dead
        for (CarAI car : cars) {
            if (car.isAlive()) {
                return false;
            }
        }
        return true;
    }

    public void tick() {
        int partitionSize = (int)Math.ceil(cars.size() / (float)Runtime.getRuntime().availableProcessors());
        Lists.partition(cars, partitionSize)
                .stream()
                .map(Task::new)
                .forEach(pool::submit);
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
            cars.stream()
                .filter(CarAI::isAlive)
                .forEach(CarAI::calculate);
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
            System.out.println("New best car: " + bestCar.getId());
        }
        return Optional.ofNullable(bestCar)
                .map(CarAI::cloneForReplay);
    }

    public CarAI selectRandomParent() {  //selects a random number in range of the fitnesssum and if a car falls in that range then select it
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

    public void naturalSelection() {
        List<CarAI> newCars = new ArrayList<>(cars.size());

        double oldFitness = bestFitness;

        newCars.add(getBest()
                .orElseGet(this::getRandomCar));

        for (int i = 1; i < cars.size(); i++) {
            CarAI child = selectRandomParent().crossoverAndMutate(selectRandomParent());
            newCars.add(child);
        }
        cars.clear();
        cars.addAll(newCars);
        gen += 1;
    }
//
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
