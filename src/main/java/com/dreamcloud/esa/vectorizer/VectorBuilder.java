package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.tfidf.CollectionInfo;
import com.dreamcloud.esa.tfidf.DocumentScoreReader;
import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfScore;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class VectorBuilder {
    Map<String, ConceptVector> cache = new ConcurrentHashMap<>();
    DocumentScoreReader scoreReader;
    TfIdfAnalyzer tfIdfAnalyzer;
    PruneOptions pruneOptions;
    DocumentPreprocessor preprocessor;
    CollectionInfo collectionInfo;

    public VectorBuilder(DocumentScoreReader scoreReader, CollectionInfo collectionInfo, TfIdfAnalyzer analyzer, DocumentPreprocessor preprocessor, PruneOptions pruneOptions) {
        this.scoreReader = scoreReader;
        this.tfIdfAnalyzer = analyzer;
        this.collectionInfo = collectionInfo;
        this.preprocessor = preprocessor;
        this.pruneOptions = pruneOptions;
    }

    public VectorBuilder(DocumentScoreReader scoreReader, CollectionInfo collectionInfo, TfIdfAnalyzer tfIdfAnalyzer, DocumentPreprocessor preprocessor) {
        this(scoreReader, collectionInfo, tfIdfAnalyzer, preprocessor, null);
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
            }

            //We need to prune these scores!
            Vector<TfIdfScore> scoreDocs = new Vector<>();
            if (pruneOptions != null && pruneOptions.windowSize > 0) {
                Vector<TfIdfScore> allTermScores = new Vector<>();
                for (String term: terms) {
                    Vector<TfIdfScore> termScores = new Vector<>();
                    scoreReader.getTfIdfScores(term, termScores);
                    if (pruneOptions.dropOff == 1.0) {
                        allTermScores.addAll(termScores.subList(0, Math.min(pruneOptions.windowSize, termScores.size())));
                    } else {
                        for (int scoreIdx = 0; scoreIdx < termScores.size(); scoreIdx++) {
                            allTermScores.add(termScores.get(scoreIdx));
                            if (scoreIdx >= pruneOptions.windowSize) {
                                float headScore = (float) termScores.get(scoreIdx - pruneOptions.windowSize).getScore();
                                float tailScore = (float) termScores.get(scoreIdx).getScore();
                                if (headScore - tailScore < headScore * pruneOptions.dropOff) {
                                    break;
                                }
                            }
                        }
                    }
                }
                scoreDocs = allTermScores;
            } else {
                scoreReader.getTfIdfScores(terms, scoreDocs);
                //Quick and dirty cutoff test
                scoreDocs.sort((t1, t2) -> Double.compare(t2.getScore(), t1.getScore()));
                scoreDocs = new Vector<>(scoreDocs.subList(0, Math.min(450, scoreDocs.size())));
            }

            for (TfIdfScore docScore: scoreDocs) {
                double score = docScore.getScore();
                score *= scoreMap.get(docScore.getTerm());
                if (Double.isNaN(score) || score <= 0) {
                    System.out.println("term score bad: " + docScore.getTerm());
                }
                vector.addScore(docScore.getDocument(), (float) score);
            }
            return vector;
            //cache.put(document, vector);
        }

        return cache.get(document);
    }
}
