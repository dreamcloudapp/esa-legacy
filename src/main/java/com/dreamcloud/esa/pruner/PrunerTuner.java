package com.dreamcloud.esa.pruner;

import com.dreamcloud.esa.tools.PValueCalculator;
import com.dreamcloud.esa.tools.SemanticSimilarityTool;
import com.dreamcloud.esa.vectorizer.PruneOptions;

import java.text.NumberFormat;
import java.util.ArrayList;

public class PrunerTuner {
    protected SemanticSimilarityTool similarity;

    public PrunerTuner(SemanticSimilarityTool similarity) {
        this.similarity = similarity;
    }

    public PrunerTuning tune(PValueCalculator pValueCalculator, PruneOptions pruneOptions, int windowStart, int windowEnd, int windowStep, double dropOffStart, double dropOffEnd, double dropOffStep) throws Exception {
        double initialDropOffStart = dropOffStart;
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(3);

        double bestScore = 0;
        int bestWindowSize = 0;
        double bestDropOff = 0;

        int iterationIdx = 0;
        int iterationCount = (((windowEnd - windowStart) / windowStep) + 1) * (int) (((dropOffEnd - dropOffStart) / dropOffStep) + 1);

        while (windowStart <= windowEnd) {
            dropOffStart = initialDropOffStart;
            ArrayList<Double> lastScores = new ArrayList<>();
            while (dropOffStart <= dropOffEnd) {
                //Change prune options (same object as in the pvalue calculator!)
                pruneOptions.windowSize = windowStart;
                pruneOptions.dropOff = (float) dropOffStart;
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
                    if (lowerScores >= 7) {
                        System.out.println("skipping (9)");
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
