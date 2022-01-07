package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.database.DocumentScore;
import com.dreamcloud.esa.fs.TermIndex;
import com.dreamcloud.esa.fs.TermIndexEntry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TfIdfAnalyzer {
    protected final Analyzer analyzer;
    protected CollectionInfo collectionInfo;

    public TfIdfAnalyzer(Analyzer analyzer, CollectionInfo collectionInfo) {
        this.analyzer = analyzer;
        this.collectionInfo = collectionInfo;
    }

    public TfIdfScore[] getTfIdfScores(String text) throws IOException {
        MutableObjectIntMap<String> termFrequencies = ObjectIntMaps.mutable.empty();
        TokenStream tokens = analyzer.tokenStream("text", text);
        CharTermAttribute termAttribute = tokens.addAttribute(CharTermAttribute.class);
        tokens.reset();
        while(tokens.incrementToken()) {
            String term = termAttribute.toString();
            if (term.length() > 32) {
                continue;
            }
            termFrequencies.addToValue(term, 1);
        }
        tokens.close();
        TfIdfScore[] scores = new TfIdfScore[termFrequencies.size()];
        int i = 0;
        for (String term: termFrequencies.keySet()) {
            //1 = term frequency with log normalization
            double tf = 1 + Math.log(termFrequencies.get(term));

            //t = inverse document frequency with log normalization
            double idf = 0;
            if (collectionInfo.hasDocumentFrequency(term)) {
                idf = Math.log(collectionInfo.numDocs / (double) collectionInfo.getDocumentFrequency(term));
            }

            //Add score
            scores[i++] = new TfIdfScore(term, tf * idf);
        }

        //c = cosine normalization
        double scoreSumOfSquares = 0.0;
        for (TfIdfScore score: scores) {
            scoreSumOfSquares += Math.pow(score.getScore(), 2);
        }
        scoreSumOfSquares = Math.sqrt(scoreSumOfSquares);

        for (TfIdfScore score: scores) {
            score.normalizeScore(1.0 / scoreSumOfSquares);
        }

        return scores;
    }
}
