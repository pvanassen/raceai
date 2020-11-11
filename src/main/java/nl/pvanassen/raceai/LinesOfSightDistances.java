package nl.pvanassen.raceai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LinesOfSightDistances {

    private final double distanceRight;

    private final double distanceAhead;

    private final double distanceLeft;

}
