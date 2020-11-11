package nl.pvanassen.raceai;

import nl.pvanassen.raceai.ai.CarAI;
import nl.pvanassen.raceai.ai.Matrix;
import nl.pvanassen.raceai.ai.NeuralNet;
import nl.pvanassen.raceai.ai.NeuralNetDebugInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class DebugPanel extends JPanel implements BiConsumer<double[], double[]> {

    private final NeuralNet neuralNet;

    private final int x = 0;

    private final int y = 0;

    private final int h;

    private final int w;

    private final BufferedImage buffer;

    private final int nSize = 60;

    private final Map<Integer, String> inputLookupMap = Map.of(0, "Speed", 1, "Dir.", 2, "Dist. right", 3, "Dist. ahead", 4, "Dist. left");

    DebugPanel(CarAI carAI) {
        this.neuralNet = carAI.getBrain();

        NeuralNetDebugInfo neuralNetDebugInfo = neuralNet.getDebugInfo();
        int maxNodes = max(neuralNetDebugInfo.getHNodes(), neuralNetDebugInfo.getINodes(), neuralNetDebugInfo.getONodes());

        setPreferredSize(new Dimension((2 + neuralNetDebugInfo.getHLayers()) * (nSize + 25), maxNodes * (nSize + 25)));
        this.w = (int)getPreferredSize().getWidth();
        this.h = (int)getPreferredSize().getHeight();
        carAI.setVisionDecisionConsumer(this);
        buffer = new BufferedImage(w, h, TYPE_INT_RGB);
    }

    @Override
    public void accept(double[] vision, double[] decision) {
        Graphics2D graphics = (Graphics2D) buffer.getGraphics();
        graphics.setBackground(new Color(0, 0, 0, 0));
        graphics.clearRect(0,0, buffer.getWidth(), buffer.getHeight());

        NeuralNetDebugInfo neuralNetDebugInfo = neuralNet.getDebugInfo();

        int iNodes = neuralNetDebugInfo.getINodes();
        int hNodes = neuralNetDebugInfo.getHNodes();
        int oNodes = neuralNetDebugInfo.getONodes();
        int hLayers = neuralNetDebugInfo.getHLayers();

        Matrix[] weights = neuralNetDebugInfo.getWeights();

        float space = 5;
        float nSpace = (w - ((weights.length + 1) * nSize)) / (float)(weights.length + 1);
        float iBuff = (h - (space * (hNodes - 1)) - (nSize * iNodes)) / 2f;
        float hBuff = (h - (space * (hNodes - 1)) - (nSize * hNodes)) / 2f;
        float oBuff = (h - (space * (oNodes - 1)) - (nSize * oNodes)) / 2f;

        double max = 0;
        int maxIndex = -1;
        for (int i = 0; i < decision.length; i++) {
            if (decision[i] > max) {
                max = decision[i];
                maxIndex = i;
            }
        }

        int lc = 0;  //Layer Count

        //DRAW NODES
        for (int i = 0; i < iNodes; i++) {  //DRAW INPUTS
            graphics.setColor(Color.WHITE);
            graphics.fillOval(x, (int)(iBuff  + y + (i * (nSize + space))), nSize, nSize);
            graphics.setFont(graphics.getFont().deriveFont(12f));
            graphics.setColor(Color.BLACK);
            graphics.drawString(String.format("%.2f", vision[i]), -25 + x + (nSize / 2f), iBuff - 5 + y + (nSize / 2f) + (i * (nSize + space)));
            graphics.drawString(inputLookupMap.get(i), -25 + x + (nSize / 2f), iBuff + 5 + y + (nSize / 2f) + (i * (nSize + space)));
        }

        lc++;

        for (int a = 0; a < hLayers; a++) {
            for (int i = 0; i < hNodes; i++) {  //DRAW HIDDEN
                graphics.setColor(Color.WHITE);
                graphics.fillOval((int)(x + (lc * nSize) + (lc * nSpace)), (int)(y + hBuff + (i * (nSize + space))), nSize, nSize);
            }
            lc++;
        }

        for (int i = 0; i < oNodes; i++) {  //DRAW OUTPUTS
            if (i == maxIndex) {
                graphics.setColor(Color.GREEN);
            } else {
                graphics.setColor(Color.WHITE);
            }
            graphics.fillOval((int)(x + (lc * nSpace) + (lc * nSize)), (int)(y + oBuff + (i * (nSize + space))), nSize, nSize);
        }

        lc = 1;

        //DRAW WEIGHTS
        for (int i = 0; i < weights[0].getRows(); i++) {  //INPUT TO HIDDEN
            for (int j = 0; j < weights[0].getCols() - 1; j++) {
                if (weights[0].getMatrix()[i][j] < 0) {
                    graphics.setColor(Color.RED);
                } else {
                    graphics.setColor(Color.BLUE);
                }
                Line2D line = new Line2D.Float(x + nSize, iBuff + y + (nSize / 2f) + (j * (space + nSize)), x + nSize + nSpace, y + hBuff + (nSize / 2f) + (i * (space + nSize)));
                graphics.draw(line);
            }
        }

        lc++;

        for (int a = 1; a < hLayers; a++) {
            for (int i = 0; i < weights[a].getRows(); i++) {  //HIDDEN TO HIDDEN
                for (int j = 0; j < weights[a].getCols() - 1; j++) {
                    if (weights[a].getMatrix()[i][j] < 0) {
                        graphics.setColor(Color.RED);
                    } else {
                        graphics.setColor(Color.BLUE);
                    }
                    Line2D line = new Line2D.Float(x + (lc * nSize) + ((lc - 1) * nSpace),
                            y + hBuff + (nSize / 2f) + (j * (space + nSize)),
                            x + (lc * nSize) + (lc * nSpace),
                            y + hBuff + (nSize / 2f) + (i * (space + nSize)));
                    graphics.draw(line);
                }
            }
            lc++;
        }

        for (int i = 0; i < weights[weights.length - 1].getRows(); i++) {  //HIDDEN TO OUTPUT
            for (int j = 0; j < weights[weights.length - 1].getCols() - 1; j++) {
                if (weights[weights.length - 1].getMatrix()[i][j] < 0) {
                    graphics.setColor(Color.RED);
                } else {
                    graphics.setColor(Color.BLUE);
                }
                Line2D line = new Line2D.Float(x + (lc * nSize) + ((lc - 1) * nSpace),
                        y + hBuff + (nSize / 2f) + (j * (space + nSize)),
                        x + (lc * nSize) + (lc * nSpace),
                        y + oBuff + (nSize / 2f) + (i * (space + nSize)));
                graphics.draw(line);
            }
        }

        graphics.setColor(Color.BLACK);
        graphics.setFont(graphics.getFont().deriveFont(15f));
        int textX = (int)((x + (lc * nSize) + (lc * nSpace) + nSize / 2f)) - 18;
        graphics.drawString("Acc", textX, -10 + y + oBuff + (nSize / 2f));
        graphics.drawString("Left", textX, 10 + y + oBuff + (nSize / 2f));
        graphics.drawString("Acc", textX, -10 + y + oBuff + space + nSize + (nSize / 2f));
        graphics.drawString("Straight", textX, 10 + y + oBuff + space + nSize + (nSize / 2f));
        graphics.drawString("Acc", textX, -10 + y + oBuff + (2 * space) + (2 * nSize) + (nSize / 2f));
        graphics.drawString("Right", textX, 10 + y + oBuff + (2 * space) + (2 * nSize) + (nSize / 2f));
        graphics.drawString("Idle", textX, -10 + y + oBuff + (3 * space) + (3 * nSize) + (nSize / 2f));
        graphics.drawString("Left", textX, 10 + y + oBuff + (3 * space) + (3 * nSize) + (nSize / 2f));
        graphics.drawString("Idle", textX, -10 + y + oBuff + (4 * space) + (4 * nSize) + (nSize / 2f));
        graphics.drawString("Straight", textX, 10 + y + oBuff + (4 * space) + (4 * nSize) + (nSize / 2f));
        graphics.drawString("Idle", textX, -10 + y + oBuff + (5 * space) + (5 * nSize) + (nSize / 2f));
        graphics.drawString("Right", textX, 10 + y + oBuff + (5 * space) + (5 * nSize) + (nSize / 2f));
        graphics.drawString("Dec", textX, -10 + y + oBuff + (6 * space) + (6 * nSize) + (nSize / 2f));
        graphics.drawString("Left", textX, 10 + y + oBuff + (6 * space) + (6 * nSize) + (nSize / 2f));
        graphics.drawString("Dec", textX, -10 + y + oBuff + (7 * space) + (7 * nSize) + (nSize / 2f));
        graphics.drawString("Straight", textX, 10 + y + oBuff + (7 * space) + (7 * nSize) + (nSize / 2f));
        graphics.drawString("Dec", textX, -10 + y + oBuff + (8 * space) + (8 * nSize) + (nSize / 2f));
        graphics.drawString("Right", textX, 10 + y + oBuff + (8 * space) + (8 * nSize) + (nSize / 2f));

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
