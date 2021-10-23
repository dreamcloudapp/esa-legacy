package com.dreamcloud.esa.similarity;

import org.apache.lucene.search.similarities.TFIDFSimilarity;

public class TrueTFIDFSimilarity  extends TFIDFSimilarity {
    public float tf(float freq) {
        return freq;
    }

    public float idf(long docFreq, long docCount) {
        return (float) docCount / docFreq;
    }

    public float lengthNorm(int numTerms) {
        //TF-IDF doesn't have a length norm does it?
        //try 1.0 and see what happens next
        return 1.0f / numTerms;
    }
}
