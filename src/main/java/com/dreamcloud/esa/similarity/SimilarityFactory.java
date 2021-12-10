package com.dreamcloud.esa.similarity;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class SimilarityFactory {
    public static String algorithm = "";

    public static Similarity getSimilarity() {
        switch (algorithm) {
            case "trueBM25":
                return new TrueBM25Similarity();
            case "trueTFIDF":
                return new TrueTFIDFSimilarity();
            case "TFIDF":
                return new ClassicSimilarity();
            case "BM25":
            default:
                return new BM25Similarity();
        }
    }
}
