package com.dreamcloud.esa.tfidf.strategy.idf;

import com.dreamcloud.esa.tfidf.strategy.InverseDocumentFrequencyStrategy;

public class NoInverseDocumentFrequency implements InverseDocumentFrequencyStrategy {
    public double idf(int totalDocs, int termDocs) {
        return 1;
    }
}
