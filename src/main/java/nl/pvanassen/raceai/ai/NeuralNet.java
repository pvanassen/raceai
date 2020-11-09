package nl.pvanassen.raceai.ai;

import com.google.gson.Gson;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicInteger;

public class NeuralNet {

    private static final AtomicInteger netNumber = new AtomicInteger(0);

    private final int iNodes;

    private final int hNodes;

    private final int oNodes;

    private final int hLayers;

    private final Matrix[] weights;

    @Getter
    private final String id;

    NeuralNet(int input, int hidden, int output, int hiddenLayers, String id) {
        this.id = id;
        iNodes = input;
        hNodes = hidden;
        oNodes = output;
        hLayers = hiddenLayers;

        weights = new Matrix[hLayers + 1];
        weights[0] = new Matrix(hNodes, iNodes + 1);
        for (int i = 1; i < hLayers; i++) {
            weights[i] = new Matrix(hNodes, hNodes + 1);
        }
        weights[weights.length - 1] = new Matrix(oNodes, hNodes + 1);
    }

    public double[] output(double[] inputsArr) {
        Matrix inputs = weights[0].singleColumnMatrixFromArray(inputsArr);

        Matrix curr_bias = inputs.addBias();

        for (int i = 0; i < hLayers; i++) {
            Matrix hidden_ip = weights[i].dot(curr_bias);
            Matrix hidden_op = hidden_ip.activate();
            curr_bias = hidden_op.addBias();
        }

        Matrix output_ip = weights[weights.length - 1].dot(curr_bias);
        Matrix output = output_ip.activate();

        return output.toArray();
    }

    public NeuralNet crossoverAndMutate(NeuralNet partner, float mr) {
        NeuralNet child = new NeuralNet(iNodes, hNodes, oNodes, hLayers, "Crossover " + netNumber.getAndIncrement());
        for (int i = 0; i < weights.length; i++) {
            child.weights[i] = weights[i].crossoverAndMutate(partner.weights[i], mr);
        }
        return child;
    }

    public NeuralNet copy() {
        NeuralNet clone = new NeuralNet(iNodes, hNodes, oNodes, hLayers, id);
        for (int i = 0; i < weights.length; i++) {
            clone.weights[i] = weights[i].copy();
        }

        return clone;
    }

    String toJson() {
        return new Gson().toJson(this);
    }

    static NeuralNet fromJson(String json) {
        return new Gson().fromJson(json, NeuralNet.class);
    }

    public NeuralNetDebugInfo getDebugInfo() {
        return NeuralNetDebugInfo.builder()
                .hLayers(hLayers)
                .hNodes(hNodes)
                .iNodes(iNodes)
                .oNodes(oNodes)
                .weights(weights)
                .build();
    }
}
