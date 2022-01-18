package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.tfidf.CollectionInfo;
import com.dreamcloud.esa.tfidf.DocumentScoreReader;
import com.dreamcloud.esa.tfidf.TfIdfAnalyzer;
import com.dreamcloud.esa.tfidf.TfIdfScore;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class VectorBuilder {
    public static Map<String, ConceptVector> cache = new ConcurrentHashMap<>();
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
        String originalDocument = document;
        if (!cache.containsKey(originalDocument)) {
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
                    for (int scoreIdx = 0; scoreIdx < termScores.size(); scoreIdx++) {
                        allTermScores.add(termScores.get(scoreIdx));
                        if (scoreIdx + pruneOptions.windowSize < termScores.size()) {
                            float headScore = (float) termScores.get(scoreIdx).getScore();
                            float tailScore = (float) termScores.get(scoreIdx + pruneOptions.windowSize).getScore();
                            if (headScore - tailScore < headScore * pruneOptions.dropOff) {
                                break;
                            }
                        }
                    }
                }
                scoreDocs = allTermScores;
            } else {
                scoreReader.getTfIdfScores(terms, scoreDocs);
            }

            for (TfIdfScore docScore: scoreDocs) {
                double weight = scoreMap.get(docScore.getTerm());
                docScore.normalizeScore(weight);
                vector.addScore(docScore.getDocument(), (float) docScore.getScore());
            }

            if (pruneOptions != null && pruneOptions.vectorLimit > 0) {
                TfIdfScore[] sortedScores = new TfIdfScore[vector.documentScores.size()];
                int s = 0;
                for (Integer documentId: vector.documentScores.keySet()) {
                    sortedScores[s++] = new TfIdfScore(documentId, null, vector.getScore(documentId));
                }

                Arrays.sort(sortedScores, (t1, t2) -> Float.compare((float) t2.getScore(), (float) t1.getScore()));
                vector.documentScores.clear();
                int cutOff = Math.min(pruneOptions.vectorLimit, sortedScores.length);
                for (int t=0; t<cutOff; t++) {
                    vector.addScore(sortedScores[t].getDocument(), (float) sortedScores[t].getScore());
                }
            }

            //return vector;
            cache.put(originalDocument, vector);
        }

        return cache.get(originalDocument);
    }
}
