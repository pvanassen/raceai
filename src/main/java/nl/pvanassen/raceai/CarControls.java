package nl.pvanassen.raceai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CarControls {

    private final Accelerate accelerate;

    private final Turn turn;
}
