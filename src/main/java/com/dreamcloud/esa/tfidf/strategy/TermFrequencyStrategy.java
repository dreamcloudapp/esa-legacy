package com.dreamcloud.esa.tfidf.strategy;

import com.dreamcloud.esa.tfidf.TermInfo;

public interface TermFrequencyStrategy {
    double tf(double tf, TermInfo termInfo);
}
