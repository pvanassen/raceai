package nl.pvanassen.raceai;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.function.Consumer;

@Getter
@Builder
@EqualsAndHashCode
public class LinesOfSight {

    private final int x;

    private final int y;

    private final int direction;
}
