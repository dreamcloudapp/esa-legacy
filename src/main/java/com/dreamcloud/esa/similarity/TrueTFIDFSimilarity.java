package com.dreamcloud.esa.similarity;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.search.similarities.TFIDFSimilarity;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity.SimScorer;
import org.apache.lucene.util.SmallFloat;
import org.apache.lucene.util.SmallFloat;

import java.util.ArrayList;
import java.util.List;

/*public class TrueTFIDFSimilarity  extends TFIDFSimilarity {
    public float tf(float freq) {
        return freq;
    }

    public float idf(long docFreq, long docCount) {
        return (docCount - docFreq) / (float) docFreq;
    }
    public float lengthNorm(int numTerms) {
        return 1.0f / numTerms;
    }
    //return (float)(1.0D / Math.sqrt((double)numTerms))
}*/

public class TrueTFIDFSimilarity extends Similarity {
    public TrueTFIDFSimilarity() {
    }

    @Override
    public long computeNorm(FieldInvertState fieldInvertState) {
        return 1;
    }

    public double idf(CollectionStatistics collectionStats, TermStatistics termStats) {
        long df = termStats.docFreq();
        long docCount = collectionStats.docCount();
        return (docCount - df) / (double) df;
    }

    public double idf(CollectionStatistics collectionStats, TermStatistics[] termStats) {
        double idf = 0.0D;
        for (TermStatistics termStat: termStats) {
            idf += this.idf(collectionStats, termStat);
        }
        return idf;
    }

    public final SimScorer scorer(float boost, CollectionStatistics collectionStats, TermStatistics... termStats) {
        double idf = termStats.length == 1 ? this.idf(collectionStats, termStats[0]) : this.idf(collectionStats, termStats);
        return new RealTFIDFScorer(boost, (float) idf);
    }

    static class RealTFIDFScorer extends SimScorer {
        private final float idf;
        private final float boost;

        public RealTFIDFScorer(float boost, float idf) {
            this.idf = idf;
            this.boost = boost;
        }

        public float score(float freq, long norm) {
            return freq * idf * boost;
        }
    }
}
