package nl.pvanassen.raceai;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.awt.geom.Line2D;
import java.util.function.Consumer;

@Getter
@Builder
public class LinesOfSightWithCallback {

    private final LinesOfSight linesOfSight;

    private final Consumer<LinesOfSightDistances> linesOfSightDistances;
}
