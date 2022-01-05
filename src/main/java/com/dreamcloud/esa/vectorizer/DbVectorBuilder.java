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
    int documentLimit;
    PruneOptions pruneOptions;

    public DbVectorBuilder(TfIdfScoreRepository scoreRepository, Analyzer analyzer, int documentLimit, PruneOptions pruneOptions) {
        this.scoreRepository = scoreRepository;
        this.tfIdfAnalyzer = new TfIdfAnalyzer(analyzer);
        this.documentLimit = documentLimit;
        this.pruneOptions = pruneOptions;

        scoreRepository.getTermDocumentFrequencies(tfIdfAnalyzer);
    }

    public DbVectorBuilder(TfIdfScoreRepository scoreRepository, Analyzer analyzer, int documentLimit) {
        this(scoreRepository, analyzer, documentLimit, null);
    }

    public ConceptVector build(String document) throws IOException {
        ConceptVector vector = new ConceptVector();
        TfIdfScore[] scores = tfIdfAnalyzer.getTfIdfScores(document);
        //Map<String, Double> scoreMap = new HashMap<>();
        //String []terms = new String[scores.length];
        int i = 0;
        for (TfIdfScore score: scores) {
            /*terms[i++] = score.getTerm();
            scoreMap.put(score.getTerm(), score.getScore());*/
            TfIdfScore[] termScores = scoreRepository.getTfIdfScores(score.getTerm());
            ConceptVector termVector = new ConceptVector();
            for (TfIdfScore termScore: termScores) {
                termVector.conceptWeights.put(termScore.getDocument(), (float) (termScore.getScore() * score.getScore()));
            }
            if (pruneOptions != null) {
                termVector = termVector.prune(pruneOptions.windowSize, pruneOptions.dropOff);
            }
            vector.merge(termVector);
        }

        /*TfIdfScore[] scoreDocs = scoreRepository.getTfIdfScores(terms);

        for (TfIdfScore docScore: scoreDocs) {
            double score = docScore.getScore();
            score *= scoreMap.get(docScore.getTerm());
            if (vector.conceptWeights.containsKey(docScore.getDocument())) {
                score += vector.conceptWeights.get(docScore.getDocument());
            }
            vector.conceptWeights.put(docScore.getDocument(), (float) score);
        }*/

        if (documentLimit > 0) {
            return vector.pruneToSize(documentLimit);
        } else {
            return vector;
        }
    }
}
