package com.dreamcloud.esa.analyzer;

import org.apache.lucene.search.similarities.BM25Similarity;

public class TrueBM25Similarity extends BM25Similarity {
    @Override
    protected float idf(long docFreq, long docCount) {
        return (docCount - docFreq) / (float) docFreq;
    }
}
