package com.dreamcloud.esa.tfidf.strategy.idf;

import com.dreamcloud.esa.tfidf.strategy.InverseDocumentFrequencyStrategy;

public class ProbabilisticInverseDocumentFrequency implements InverseDocumentFrequencyStrategy {
    public double idf(int totalDocs, int termDocs) {
        return Math.max(0, Math.log((double) (totalDocs - termDocs) / (double) termDocs));
    }
}
