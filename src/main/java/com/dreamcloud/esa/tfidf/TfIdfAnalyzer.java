package com.dreamcloud.esa.tfidf;

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
    protected AtomicInteger documentCount = new AtomicInteger(0);
    protected final MutableObjectIntMap<String> documentFrequencies = ObjectIntMaps.mutable.empty();
    protected final Analyzer analyzer;

    public TfIdfAnalyzer(Analyzer analyzer, String smartFlags) {
        this.analyzer = analyzer;
    }

    public TfIdfAnalyzer(Analyzer analyzer) {
        this(analyzer, "1tc");
    }

    /**
     * Processes document terms for IDF calculation.
     * @param text Any text document containing terms
     */
    public void prepareDocument(String text) throws IOException {
        TokenStream tokens = analyzer.tokenStream("text", text);
        CharTermAttribute termAttribute = tokens.addAttribute(CharTermAttribute.class);
        Set<String> uniqueTerms = new HashSet<>();
        tokens.reset();
        while(tokens.incrementToken()) {
            String term = termAttribute.toString();
            if (term.length() > 32) {
                continue;
            }
            uniqueTerms.add(term);
        }
        tokens.close();
        synchronized (documentFrequencies) {
            for (String uniqueTerm: uniqueTerms) {
                documentFrequencies.addToValue(uniqueTerm, 1);
            }
        }
        documentCount.incrementAndGet();
    }

    public MutableObjectIntMap<String> getDocumentFrequencies() {
        return this.documentFrequencies;
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
            double idf = Math.log(documentCount.get() / (double) documentFrequencies.get(term));

            //Add score
            double tfidf = tf * idf;
            if (Double.isNaN(tfidf)) {
                System.out.println("0-tfidf: tf=" + tf + "; idf=" + idf);
            }
            scores[i++] = new TfIdfScore(term, tf * idf);
        }

        ////c = cosine normalization
        double scoreSumOfSquares = 0.0;
        for (TfIdfScore score: scores) {
            scoreSumOfSquares += Math.pow(score.getScore(), 2);
        }
        scoreSumOfSquares = Math.sqrt(scoreSumOfSquares);

        if (Double.isNaN(scoreSumOfSquares)) {
            System.out.println("NaN sum: " + termFrequencies.size());
        }

        if (scoreSumOfSquares == 0) {
            System.out.println("Zero sum: " + termFrequencies.size());
        }

        for (TfIdfScore score: scores) {
            score.normalizeScore(1.0 / scoreSumOfSquares);
        }

        for (TfIdfScore score: scores) {
            if (Double.isNaN(score.getScore())) {
                System.out.println("NaN score: " + score.getTerm());
                System.exit(1);
            }
        }

        return scores;
    }
}
