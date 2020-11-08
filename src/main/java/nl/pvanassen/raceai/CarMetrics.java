package nl.pvanassen.raceai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CarMetrics {

    private final double speed;

    private final double direction;

    private final double distanceRight;

    private final double distanceAhead;

    private final double distanceLeft;

    public double[] getInput() {
        double[] input = new double[5];
        input[0] = speed;
        input[1] = direction;
        input[2] = distanceRight;
        input[3] = distanceAhead;
        input[4] = distanceLeft;
        return input;
    }
}
