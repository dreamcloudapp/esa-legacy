package com.dreamcloud.esa.tfidf.strategy;

public interface InverseDocumentFrequencyStrategy {
    double idf(int totalDocs, int termDocs);
}
