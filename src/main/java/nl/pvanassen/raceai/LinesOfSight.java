package nl.pvanassen.raceai;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.awt.geom.Line2D;
import java.util.function.Consumer;

@Getter
@Builder
@EqualsAndHashCode
public class LinesOfSight {

    private final double x;

    private final double y;

    private final double direction;

    private final Line2D.Double lineOfSightRight;

    private final Line2D.Double lineOfSightAhead;

    private final Line2D.Double lineOfSightLeft;

    private final Consumer<LinesOfSightDistances> linesOfSightDistances;
}
