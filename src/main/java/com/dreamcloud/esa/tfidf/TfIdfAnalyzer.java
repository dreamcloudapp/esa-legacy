package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.database.DocumentScore;
import com.dreamcloud.esa.fs.TermIndex;
import com.dreamcloud.esa.fs.TermIndexEntry;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TfIdfAnalyzer {
    TfIdfCalculator calculator;
    protected final Analyzer analyzer;
    protected CollectionInfo collectionInfo;

    public TfIdfAnalyzer(TfIdfCalculator calculator, Analyzer analyzer, CollectionInfo collectionInfo) {
        this.calculator = calculator;
        this.analyzer = analyzer;
        this.collectionInfo = collectionInfo;
    }

    public TfIdfScore[] getTfIdfScores(String text) throws IOException {
        MutableObjectIntMap<String> termFrequencies = ObjectIntMaps.mutable.empty();
        TokenStream tokens = analyzer.tokenStream("text", text);
        CharTermAttribute termAttribute = tokens.addAttribute(CharTermAttribute.class);
        tokens.reset();
        while(tokens.incrementToken()) {
            if (collectionInfo.getDocumentFrequency(termAttribute.toString()) == 0) {
                //there is nothing in our vectors about this term, so ignore it
                continue;
            }
            termFrequencies.addToValue(termAttribute.toString(), 1);
        }
        tokens.close();
        TermInfo[] termInfos = new TermInfo[termFrequencies.size()];
        int i = 0;
        int totalTf = 0;
        int maxTf = 0;
        int totalDocs = collectionInfo.getDocumentCount();
        for (String term: termFrequencies.keySet()) {
            //1 = term frequency with log normalization
            int tf = termFrequencies.get(term);
            totalTf += tf;
            maxTf = Math.max(tf, maxTf);
            TermInfo termInfo = new TermInfo();
            termInfo.term = term;
            termInfo.tf = tf;
            termInfos[i++] = termInfo;
        }

        if (calculator.collectAverageTermFrequency()) {
            for (TermInfo termInfo: termInfos) {
                termInfo.avgTf = totalTf / (double) termInfos.length;
            }
        }
        if (calculator.collectMaxTermFrequency()) {
            for (TermInfo termInfo: termInfos) {
                termInfo.maxTf = maxTf;
            }
        }

        TfIdfScore[] scores = new TfIdfScore[termFrequencies.size()];
        i = 0;
        for (TermInfo termInfo: termInfos) {
            double tf = calculator.tf(termInfo.tf, termInfo);
            int termDocs = collectionInfo.getDocumentFrequency(termInfo.term);
            double idf = calculator.idf(totalDocs, termDocs);

            scores[i++] = new TfIdfScore(termInfo.term, tf * idf);
        }

        double norm = calculator.norm(scores);
        for (TfIdfScore score: scores) {
            score.normalizeScore(norm);
        }

        return scores;
    }
}
