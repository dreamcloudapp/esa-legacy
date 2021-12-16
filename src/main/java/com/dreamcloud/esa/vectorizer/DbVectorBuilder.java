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

        MutableObjectIntMap<String> documentFrequencies = scoreRepository.getTermDocumentFrequencies();
        tfIdfAnalyzer.setDocumentFrequencies(documentFrequencies);
    }

    public ConceptVector build(String document) throws IOException {
        ConceptVector vector = new ConceptVector();
        TfIdfScore[] scores = tfIdfAnalyzer.getTfIdfScores(document);
        for (TfIdfScore score: scores) {
            ConceptVector termVector = new ConceptVector();
            TfIdfScore[] docScores = scoreRepository.getTfIdfScores(score.getTerm());
            for (TfIdfScore docScore: docScores) {
                termVector.conceptWeights.put(docScore.getDocument(), (float) (score.getScore() * docScore.getScore()));
            }
            vector.merge(termVector);
        }
        return vector;
    }
}
