package com.dreamcloud.esa.tfidf.strategy.tf;

import com.dreamcloud.esa.tfidf.TermInfo;
import com.dreamcloud.esa.tfidf.strategy.TermFrequencyStrategy;

public class AugmentedTermFrequency implements TermFrequencyStrategy {
    public double tf(double tf, TermInfo termInfo) {
        return 0.5 + ((0.5 * tf) / termInfo.maxTf);
    }
}
