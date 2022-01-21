package com.dreamcloud.esa.tfidf.strategy.idf;

import com.dreamcloud.esa.tfidf.strategy.InverseDocumentFrequencyStrategy;

public class LinearInverseDocumentFrequency implements InverseDocumentFrequencyStrategy {
    @Override
    public double idf(int totalDocs, int termDocs) {
        return totalDocs / (double) termDocs;
    }
}
