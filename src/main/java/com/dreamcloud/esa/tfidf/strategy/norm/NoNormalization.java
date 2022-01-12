package com.dreamcloud.esa.tfidf.strategy.norm;

import com.dreamcloud.esa.tfidf.TfIdfScore;
import com.dreamcloud.esa.tfidf.strategy.NormalizationStrategy;

public class NoNormalization implements NormalizationStrategy {
    public double norm(TfIdfScore[] scores) {
        return 1;
    }
}
