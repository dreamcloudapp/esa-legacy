package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.tfidf.strategy.TfIdfStrategy;

public class BM25Calculator implements TfIdfStrategy {
    TfIdfStrategy tfIdfStrategy;
    protected double k;
    protected double b;
    protected double delta;

    public BM25Calculator(TfIdfStrategy tfIdfStrategy, double k, double b, double delta) {
        this.tfIdfStrategy = tfIdfStrategy;
        this.k = k;
        this.b = b;
        this.delta = delta;
    }

    public BM25Calculator(TfIdfStrategy tfIdfStrategy, double k, double b) {
        this(tfIdfStrategy, k, b, 0.0);
    }

    public BM25Calculator(TfIdfStrategy tfIdfStrategy) {
        this(tfIdfStrategy, 1.2, 0.75, 0.0);
    }

    public double tf(double tf, TermInfo termInfo) {
        double tfScore = this.tfIdfStrategy.tf(tf, termInfo);
        return ((tfScore * (k + 1)) / (tfScore + (k * (1 - b + (b * (termInfo.dl / termInfo.avgDl)))))) + delta;
    }

    public double idf(int totalDocs, int termDocs) {
        return this.tfIdfStrategy.idf(totalDocs, termDocs);
    }

    public double norm(TfIdfScore[] scores) {
        return this.tfIdfStrategy.norm(scores);
    }
}
