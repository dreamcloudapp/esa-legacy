package com.dreamcloud.esa.tfidf.strategy.norm;

import com.dreamcloud.esa.tfidf.TfIdfScore;
import com.dreamcloud.esa.tfidf.strategy.NormalizationStrategy;

public class CosineNormalization implements NormalizationStrategy {
    public double norm(TfIdfScore[] scores) {
        double scoreSumOfSquares = 0.0;
        for (TfIdfScore score: scores) {
            scoreSumOfSquares += Math.pow(score.getScore(), 2);
        }
        return 1 / Math.sqrt(scoreSumOfSquares);
    }
}
