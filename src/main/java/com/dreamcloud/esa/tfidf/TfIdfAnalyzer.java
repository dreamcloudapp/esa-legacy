package com.dreamcloud.esa.tfidf;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;

import java.io.IOException;

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
        int dl = 0;
        double avgDl = collectionInfo.getAverageDocumentLength();
        while(tokens.incrementToken()) {
            termFrequencies.addToValue(termAttribute.toString(), 1);
            dl++;
        }
        tokens.close();
        TermInfo[] termInfos = new TermInfo[termFrequencies.size()];
        int i = 0;
        int totalTf = 0;
        int maxTf = 0;
        int totalDocs = collectionInfo.getDocumentCount();
        for (String term: termFrequencies.keySet()) {
            int tf = termFrequencies.get(term);
            totalTf += tf;
            maxTf = Math.max(tf, maxTf);
            TermInfo termInfo = new TermInfo();
            termInfo.term = term;
            termInfo.tf = tf;
            termInfo.dl = dl;
            termInfo.avgDl = avgDl;
            termInfos[i++] = termInfo;
        }

        for (TermInfo termInfo: termInfos) {
            termInfo.avgTf = totalTf / (double) termInfos.length;
            termInfo.maxTf = maxTf;
        }

        TfIdfScore[] scores = new TfIdfScore[termFrequencies.size()];
        i = 0;
        for (TermInfo termInfo: termInfos) {
            double tf = calculator.tf(termInfo.tf, termInfo);
            int termDocs = collectionInfo.getDocumentFrequency(termInfo.term);
            double idf = 0;
            if (termDocs > 0) {
                idf = calculator.idf(totalDocs, termDocs);
            }

            scores[i++] = new TfIdfScore(termInfo.term, tf * idf);
        }

        double norm = calculator.norm(scores);
        for (TfIdfScore score: scores) {
            score.normalizeScore(norm);
        }

        return scores;
    }
}
