package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.database.TfIdfScoreRepository;
import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfScore;
import org.apache.lucene.analysis.Analyzer;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;

import java.io.IOException;

public class DbVectorBuilder implements VectorBuilder {
    TfIdfScoreRepository scoreRepository;
    TfIdfAnalyzer tfIdfAnalyzer;

    public DbVectorBuilder(TfIdfScoreRepository scoreRepository, Analyzer analyzer) {
        this.scoreRepository = scoreRepository;
        this.tfIdfAnalyzer = new TfIdfAnalyzer(analyzer);

        scoreRepository.getTermDocumentFrequencies(tfIdfAnalyzer);
    }

    public ConceptVector build(String document) throws IOException {
        ConceptVector vector = new ConceptVector();
        TfIdfScore[] scores = tfIdfAnalyzer.getTfIdfScores(document);
        String []terms = new String[scores.length];
        int i = 0;
        for (TfIdfScore score: scores) {
            terms[i++] = score.getTerm();
        }
        TfIdfScore[] scoreDocs = scoreRepository.getTfIdfScores(terms);

        for (TfIdfScore docScore: scoreDocs) {
            double score = docScore.getScore();
            for (TfIdfScore termScore: scores) {
                if (termScore.getTerm().equals(docScore.getTerm())) {
                    score *= termScore.getScore();
                }
            }
            vector.conceptWeights.put(docScore.getDocument(), (float) score);
        }

        vector.pruneToSize(1600);
        return vector;
    }
}
