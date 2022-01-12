package com.dreamcloud.esa.tfidf.strategy;

import com.dreamcloud.esa.tfidf.TfIdfScore;

public interface NormalizationStrategy {
    double norm(TfIdfScore[] scores);
}
