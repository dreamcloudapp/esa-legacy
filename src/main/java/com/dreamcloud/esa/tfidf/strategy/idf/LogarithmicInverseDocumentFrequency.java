package com.dreamcloud.esa.tfidf.strategy.idf;

import com.dreamcloud.esa.tfidf.strategy.InverseDocumentFrequencyStrategy;

public class LogarithmicInverseDocumentFrequency implements InverseDocumentFrequencyStrategy {
    @Override
    public double idf(int totalDocs, int termDocs) {
        return Math.log((double) totalDocs / (double) termDocs);
    }
}
