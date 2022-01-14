package com.dreamcloud.esa.tfidf.strategy.tf;

import com.dreamcloud.esa.tfidf.TermInfo;
import com.dreamcloud.esa.tfidf.strategy.TermFrequencyStrategy;

public class LogarithmicTermFrequency implements TermFrequencyStrategy {
    public double tf(double tf, TermInfo termInfo) {
        return 1 + Math.log(tf);
    }
}
