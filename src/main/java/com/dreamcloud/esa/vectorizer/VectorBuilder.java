package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.tfidf.CollectionInfo;
import com.dreamcloud.esa.tfidf.DocumentScoreReader;
import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfScore;
import org.apache.lucene.analysis.Analyzer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VectorBuilder {
    Map<String, ConceptVector> cache = new ConcurrentHashMap<>();
    DocumentScoreReader scoreReader;
    TfIdfAnalyzer tfIdfAnalyzer;
    int documentLimit;
    PruneOptions pruneOptions;
    DocumentPreprocessor preprocessor;
    CollectionInfo collectionInfo;

    public VectorBuilder(DocumentScoreReader scoreReader, CollectionInfo collectionInfo, TfIdfAnalyzer analyzer, DocumentPreprocessor preprocessor, int documentLimit, PruneOptions pruneOptions) {
        this.scoreReader = scoreReader;
        this.tfIdfAnalyzer = analyzer;
        this.collectionInfo = collectionInfo;
        this.preprocessor = preprocessor;
        this.documentLimit = documentLimit;
        this.pruneOptions = pruneOptions;
    }

    public VectorBuilder(DocumentScoreReader scoreReader, CollectionInfo collectionInfo, TfIdfAnalyzer tfIdfAnalyzer, DocumentPreprocessor preprocessor, int documentLimit) {
        this(scoreReader, collectionInfo, tfIdfAnalyzer, preprocessor, documentLimit, null);
    }

    public ConceptVector build(String document) throws Exception {
        if (!cache.containsKey(document)) {
            if (preprocessor != null) {
                document = preprocessor.process(document);
            }

            ConceptVector vector = new ConceptVector(collectionInfo.getDocumentCount());
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
                vector.addScore(docScore.getDocument(), (float) score);
            }

            if (documentLimit > 0) {
                vector.pruneToSize(documentLimit);
            }
            return vector;
            //cache.put(document, vector);
        }

        return cache.get(document);
    }
}
