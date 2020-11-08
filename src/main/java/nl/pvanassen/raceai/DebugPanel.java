package nl.pvanassen.raceai;

import nl.pvanassen.raceai.ai.CarAI;
import nl.pvanassen.raceai.ai.Matrix;
import nl.pvanassen.raceai.ai.NeuralNet;
import nl.pvanassen.raceai.ai.NeuralNetDebugInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class DebugPanel extends JPanel implements BiConsumer<double[], double[]> {

    private final NeuralNet neuralNet;

    private final CarAI carAI;

    private final int x = 0;

    private final int y = 0;

    private final int w = 600;

    private final int h = 790;

    private final BufferedImage buffer = new BufferedImage(w, h, TYPE_INT_RGB);

    private final Map<Integer, String> inputLookupMap = Map.of(0, "Speed", 1, "Dir.", 2, "Dist. right", 3, "Dist. ahead", 4, "Dist. left");

    DebugPanel(CarAI carAI) {
        this.carAI = carAI;
        this.neuralNet = carAI.getBrain();
        setPreferredSize(new Dimension(w, h));
        carAI.setVisionDecisionConsumer(this);
    }

    @Override
    public void accept(double[] vision, double[] decision) {
        NeuralNetDebugInfo neuralNetDebugInfo = neuralNet.getDebugInfo();
        int iNodes = neuralNetDebugInfo.getINodes();
        int hNodes = neuralNetDebugInfo.getHNodes();
        int oNodes = neuralNetDebugInfo.getONodes();
        int hLayers = neuralNetDebugInfo.getHLayers();

        Matrix[] weights = neuralNetDebugInfo.getWeights();

        float space = 5;
        int maxNodes = max(iNodes, hNodes, oNodes);
        int nSize = (int)((h - (space * (maxNodes - 2))) / maxNodes);
        float nSpace = (w - ((weights.length + 1) * nSize)) / (float)(weights.length + 1);
        float hBuff = (h - (space * (hNodes - 1)) - (nSize * hNodes)) / 2f;
        float oBuff = (h - (space * (oNodes - 1)) - (nSize * oNodes)) / 2f;

        int maxIndex = 0;
        for (int i = 1; i < decision.length; i++) {
            if (decision[i] > decision[maxIndex]) {
                maxIndex = i;
            }
        }

        int lc = 0;  //Layer Count

        Graphics2D parent = (Graphics2D)buffer.getGraphics();
        //DRAW NODES
        for (int i = 0; i < iNodes; i++) {  //DRAW INPUTS
            if (vision[i] != 0) {
                parent.setColor(Color.GREEN);
            } else {
                parent.setColor(Color.WHITE);
            }
            parent.fillOval(x, (int)(y + (i * (nSize + space))), nSize, nSize);
            parent.setFont(parent.getFont().deriveFont(12f));
            parent.setColor(Color.BLACK);
            parent.drawString(inputLookupMap.get(i), -25 + x + (nSize / 2f), 5 + y + (nSize / 2f) + (i * (nSize + space)));
        }

        lc++;

        for (int a = 0; a < hLayers; a++) {
            for (int i = 0; i < hNodes; i++) {  //DRAW HIDDEN
                parent.setColor(Color.WHITE);
                parent.fillOval((int)(x + (lc * nSize) + (lc * nSpace)), (int)(y + hBuff + (i * (nSize + space))), nSize, nSize);
            }
            lc++;
        }

        for (int i = 0; i < oNodes; i++) {  //DRAW OUTPUTS
            if (i == maxIndex) {
                parent.setColor(Color.GREEN);
            } else {
                parent.setColor(Color.WHITE);
            }
            parent.fillOval((int)(x + (lc * nSpace) + (lc * nSize)), (int)(y + oBuff + (i * (nSize + space))), nSize, nSize);
        }

        lc = 1;

        //DRAW WEIGHTS
        for (int i = 0; i < weights[0].getRows(); i++) {  //INPUT TO HIDDEN
            for (int j = 0; j < weights[0].getCols() - 1; j++) {
                if (weights[0].getMatrix()[i][j] < 0) {
                    parent.setColor(Color.RED);
                } else {
                    parent.setColor(Color.BLUE);
                }
                Line2D line = new Line2D.Float(x + nSize, y + (nSize / 2f) + (j * (space + nSize)), x + nSize + nSpace, y + hBuff + (nSize / 2f) + (i * (space + nSize)));
                parent.draw(line);
            }
        }

        lc++;

        for (int a = 1; a < hLayers; a++) {
            for (int i = 0; i < weights[a].getRows(); i++) {  //HIDDEN TO HIDDEN
                for (int j = 0; j < weights[a].getCols() - 1; j++) {
                    if (weights[a].getMatrix()[i][j] < 0) {
                        parent.setColor(Color.RED);
                    } else {
                        parent.setColor(Color.BLUE);
                    }
                    Line2D line = new Line2D.Float(x + (lc * nSize) + ((lc - 1) * nSpace),
                            y + hBuff + (nSize / 2f) + (j * (space + nSize)),
                            x + (lc * nSize) + (lc * nSpace),
                            y + hBuff + (nSize / 2f) + (i * (space + nSize)));
                    parent.draw(line);
                }
            }
            lc++;
        }

        for (int i = 0; i < weights[weights.length - 1].getRows(); i++) {  //HIDDEN TO OUTPUT
            for (int j = 0; j < weights[weights.length - 1].getCols() - 1; j++) {
                if (weights[weights.length - 1].getMatrix()[i][j] < 0) {
                    parent.setColor(Color.RED);
                } else {
                    parent.setColor(Color.BLUE);
                }
                Line2D line = new Line2D.Float(x + (lc * nSize) + ((lc - 1) * nSpace),
                        y + hBuff + (nSize / 2f) + (j * (space + nSize)),
                        x + (lc * nSize) + (lc * nSpace),
                        y + oBuff + (nSize / 2f) + (i * (space + nSize)));
                parent.draw(line);
            }
        }

        parent.setColor(Color.BLACK);
        parent.setFont(parent.getFont().deriveFont(15f));
        int textX = (int)((x + (lc * nSize) + (lc * nSpace) + nSize / 2f)) - 18;
        parent.drawString("Acc", textX, -10 + y + oBuff + (nSize / 2f));
        parent.drawString("Left", textX, 10 + y + oBuff + (nSize / 2f));
        parent.drawString("Acc", textX, -10 + y + oBuff + space + nSize + (nSize / 2f));
        parent.drawString("Straight", textX, 10 + y + oBuff + space + nSize + (nSize / 2f));
        parent.drawString("Acc", textX, -10 + y + oBuff + (2 * space) + (2 * nSize) + (nSize / 2f));
        parent.drawString("Right", textX, 10 + y + oBuff + (2 * space) + (2 * nSize) + (nSize / 2f));
        parent.drawString("Idle", textX, -10 + y + oBuff + (3 * space) + (3 * nSize) + (nSize / 2f));
        parent.drawString("Left", textX, 10 + y + oBuff + (3 * space) + (3 * nSize) + (nSize / 2f));
        parent.drawString("Idle", textX, -10 + y + oBuff + (4 * space) + (4 * nSize) + (nSize / 2f));
        parent.drawString("Straight", textX, 10 + y + oBuff + (4 * space) + (4 * nSize) + (nSize / 2f));
        parent.drawString("Idle", textX, -10 + y + oBuff + (5 * space) + (5 * nSize) + (nSize / 2f));
        parent.drawString("Right", textX, 10 + y + oBuff + (5 * space) + (5 * nSize) + (nSize / 2f));
        parent.drawString("Dec", textX, -10 + y + oBuff + (6 * space) + (6 * nSize) + (nSize / 2f));
        parent.drawString("Left", textX, 10 + y + oBuff + (6 * space) + (6 * nSize) + (nSize / 2f));
        parent.drawString("Dec", textX, -10 + y + oBuff + (7 * space) + (7 * nSize) + (nSize / 2f));
        parent.drawString("Straight", textX, 10 + y + oBuff + (7 * space) + (7 * nSize) + (nSize / 2f));
        parent.drawString("Dec", textX, -10 + y + oBuff + (8 * space) + (8 * nSize) + (nSize / 2f));
        parent.drawString("Right", textX, 10 + y + oBuff + (8 * space) + (8 * nSize) + (nSize / 2f));


        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(buffer, x, y, w, h, null);
    }

    private int max(int...values) {
        int max = Integer.MIN_VALUE;
        for (int value : values) {
            if (max < value) {
                max = value;
            }
        }
        return max;
    }
}
