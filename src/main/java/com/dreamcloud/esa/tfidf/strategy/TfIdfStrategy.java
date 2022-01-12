package com.dreamcloud.esa.tfidf.strategy;

import com.dreamcloud.esa.tfidf.TermInfo;
import com.dreamcloud.esa.tfidf.TfIdfScore;

public interface TfIdfStrategy {
    double tf(double tf, TermInfo termInfo);
    double idf(int totalDocs, int termDocs);
    double norm(TfIdfScore[] scores);
}
