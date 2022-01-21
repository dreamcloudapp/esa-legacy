package com.dreamcloud.esa.tfidf.strategy.tf;

import com.dreamcloud.esa.tfidf.TermInfo;
import com.dreamcloud.esa.tfidf.strategy.TermFrequencyStrategy;

public class Pow4TermFrequency implements TermFrequencyStrategy {
    public double tf(double tf, TermInfo termInfo) {
        return Math.pow(tf, 4);
    }
}
