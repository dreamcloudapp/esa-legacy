package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.tfidf.DocumentScoreReader;
import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.util.HashMap;
import java.util.Map;

public class VectorBuilder {
    DocumentScoreReader scoreReader;
    TfIdfAnalyzer tfIdfAnalyzer;
    int documentLimit;
    PruneOptions pruneOptions;
    DocumentPreprocessor preprocessor;

    public VectorBuilder(DocumentScoreReader scoreReader, TfIdfAnalyzer tfIdfAnalyzer, DocumentPreprocessor preprocessor, int documentLimit, PruneOptions pruneOptions) {
        this.scoreReader = scoreReader;
        this.tfIdfAnalyzer = tfIdfAnalyzer;
        this.preprocessor = preprocessor;
        this.documentLimit = documentLimit;
        this.pruneOptions = pruneOptions;
    }

    public VectorBuilder(DocumentScoreReader scoreReader, TfIdfAnalyzer tfIdfAnalyzer, DocumentPreprocessor preprocessor, int documentLimit) {
        this(scoreReader, tfIdfAnalyzer, preprocessor, documentLimit, null);
    }

    public ConceptVector build(String document) throws Exception {
        if (preprocessor != null) {
            document = preprocessor.process(document);
        }

        ConceptVector vector = new ConceptVector();
        TfIdfScore[] scores = tfIdfAnalyzer.getTfIdfScores(document);
        Map<String, Float> scoreMap = new HashMap<>();
        String []terms = new String[scores.length];
        int i = 0;
        for (TfIdfScore score: scores) {
            terms[i++] = score.getTerm();
            scoreMap.put(score.getTerm(), (float) score.getScore());
            /*TfIdfScore[] termScores = scoreReader.getTfIdfScores(score.getTerm());
            ConceptVector termVector = new ConceptVector();
            for (TfIdfScore termScore: termScores) {
                termVector.conceptWeights.put(termScore.getDocument(), (float) (termScore.getScore() * score.getScore()));
            }
            if (pruneOptions != null) {
                //termVector = termVector.prune(pruneOptions.windowSize, pruneOptions.dropOff);
            }
            vector.merge(termVector);*/
        }

        TfIdfScore[] scoreDocs = scoreReader.getTfIdfScores(terms);

        for (TfIdfScore docScore: scoreDocs) {
            double score = docScore.getScore();
            score *= scoreMap.get(docScore.getTerm());
            if (vector.conceptWeights.containsKey(docScore.getDocument())) {
                score += vector.conceptWeights.get(docScore.getDocument());
            }
            vector.conceptWeights.put(docScore.getDocument(), (float) score);
        }

        if (documentLimit > 0) {
            return vector.pruneToSize(documentLimit);
        } else {
            return vector;
        }
    }
}
