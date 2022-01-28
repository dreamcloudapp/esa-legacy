package com.dreamcloud.esa.pruner;

import com.dreamcloud.esa.tools.PValueCalculator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrunerTuner {
    protected SemanticSimilarityTool similarity;

    public PrunerTuner(SemanticSimilarityTool similarity) {
        this.similarity = similarity;
    }

    public PrunerTuning tune(PValueCalculator pValueCalculator, PruneOptions pruneOptions, int windowStart, int windowEnd, int windowStep, double dropOffStart, double dropOffEnd, double dropOffStep) throws Exception {
        AtomicBoolean skipWindow = new AtomicBoolean(false);
        Thread skipThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                Scanner scanner = new Scanner(System.in);
                String line = scanner.nextLine();
                if ("s".equals(line)) {
                    System.out.println("skipping (user)");
                    skipWindow.set(true);
                }
            }
        });
        skipThread.setDaemon(true);
        skipThread.start();
        double initialDropOffStart = dropOffStart;
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(8);
        format.setMinimumFractionDigits(8);

        double bestScore = 0;
        int bestWindowSize = 0;
        double bestDropOff = 0;

        int iterationIdx = 0;
        int iterationCount = (((windowEnd - windowStart) / windowStep) + 1) * (int) (((dropOffEnd - dropOffStart) / dropOffStep) + 1);

        while (windowStart <= windowEnd) {
            dropOffStart = initialDropOffStart;
            ArrayList<Double> lastScores = new ArrayList<>();
            while (dropOffStart <= dropOffEnd) {
                if (skipWindow.get()) {
                    skipWindow.set(false);
                    break;
                }

                //Change prune options (same object as in the pvalue calculator!)
                pruneOptions.windowSize = windowStart;
                pruneOptions.dropOff = (float) dropOffStart;
                //hacky shmack
                VectorBuilder.cache.clear();
                double score = pValueCalculator.getSpearmanCorrelation(similarity);
                lastScores.add(score);
                if (lastScores.size() > 10) {
                    lastScores.remove(0);
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestWindowSize = windowStart;
                    bestDropOff = dropOffStart;
                    System.out.println("!!! best score !!!");
                }

                //If we have 10 scores, check to see if 7 of the 10 are lower than the previous ones
                if (lastScores.size() == 10) {
                    int lowerScores = 0;
                    for (int scoreIdx = 1; scoreIdx < 10; scoreIdx++) {
                        double currentScore = lastScores.get(scoreIdx);
                        double previousScore = lastScores.get(scoreIdx - 1);
                        if (currentScore < previousScore) {
                            lowerScores++;
                        }
                    }
                    if (lowerScores >= 8) {
                        System.out.println("skipping (9)");
                        iterationIdx += (int) (((dropOffEnd - dropOffStart) / dropOffStep) + 1);
                        break;
                    }
                }

                System.out.println(format.format(score) + ": " + windowStart + "/" + format.format(dropOffStart) + "\t[" + iterationIdx + "|" + iterationCount + "]\tbest: " + format.format(bestScore));
                dropOffStart += dropOffStep;
                iterationIdx++;
            }
            windowStart += windowStep;
        }
        return new PrunerTuning(bestScore, bestWindowSize, bestDropOff);
    }
}
