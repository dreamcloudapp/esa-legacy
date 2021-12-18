package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.database.TfIdfScoreRepository;
import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfScore;
import org.apache.lucene.analysis.Analyzer;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
        Map<String, Double> scoreMap = new HashMap<>();
        String []terms = new String[scores.length];
        int i = 0;
        for (TfIdfScore score: scores) {
            terms[i++] = score.getTerm();
            scoreMap.put(score.getTerm(), score.getScore());
        }
        TfIdfScore[] scoreDocs = scoreRepository.getTfIdfScores(terms);

        for (TfIdfScore docScore: scoreDocs) {
            double score = docScore.getScore();
            score *= scoreMap.get(docScore.getTerm());
            if (vector.conceptWeights.containsKey(docScore.getDocument())) {
                score += vector.conceptWeights.get(docScore.getDocument());
            }
            vector.conceptWeights.put(docScore.getDocument(), (float) score);
        }

        vector.pruneToSize(1600);
        return vector;
    }
}
