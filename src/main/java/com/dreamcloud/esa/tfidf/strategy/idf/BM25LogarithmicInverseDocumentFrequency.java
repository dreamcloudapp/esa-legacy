package com.dreamcloud.esa.tfidf.strategy.idf;

import com.dreamcloud.esa.tfidf.strategy.InverseDocumentFrequencyStrategy;

public class BM25LogarithmicInverseDocumentFrequency implements InverseDocumentFrequencyStrategy {
    public double idf(int totalDocs, int termDocs) {
        return Math.log(1 + ((totalDocs - termDocs + 0.5) / (termDocs + 0.5)));
    }
}
