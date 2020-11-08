package nl.pvanassen.raceai.ai;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NeuralNetDebugInfo {
    private final int iNodes;

    private final int hNodes;

    private final int oNodes;

    private final int hLayers;

    private final Matrix[] weights;
}
