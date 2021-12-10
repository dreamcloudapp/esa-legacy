package com.dreamcloud.esa.similarity;

import org.apache.lucene.search.similarities.TFIDFSimilarity;

public class TrueTFIDFSimilarity  extends TFIDFSimilarity {
    public float tf(float freq) {
        return freq;
    }

    public float idf(long docFreq, long docCount) {
        return (docCount - docFreq) / (float) docFreq;
    }
    public float lengthNorm(int numTerms) {
        return 1.0f;
    }
    //return (float)(1.0D / Math.sqrt((double)numTerms))
}
