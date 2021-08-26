package be.vanoosten.esa;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class SimilarityFactory {
    private static String similarityType = "default";

    public static void setSimilarityType(String similarityType) {
        SimilarityFactory.similarityType = similarityType;
    }

    static Similarity getSimilarity() {
        switch (similarityType) {
            case "bm25":
                return new BM25Similarity();
            case "default":
            default:
                return new DefaultSimilarity();
        }
    }
}
