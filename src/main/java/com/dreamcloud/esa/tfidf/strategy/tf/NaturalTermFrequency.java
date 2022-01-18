package com.dreamcloud.esa.tfidf.strategy.tf;

import com.dreamcloud.esa.tfidf.TermInfo;
import com.dreamcloud.esa.tfidf.strategy.TermFrequencyStrategy;

/**
 * Normal term frequency (doesn't adjust in any way).
 */
public class NaturalTermFrequency implements TermFrequencyStrategy {
    //todo: play around with this as just for querying it makes things slightly better! (0.002 or so)
    public double tf(double tf, TermInfo termInfo) {
        return Math.pow(tf, 4);
    }
}
