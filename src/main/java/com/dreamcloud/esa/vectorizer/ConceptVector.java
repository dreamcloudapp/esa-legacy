package com.dreamcloud.esa.vectorizer;

import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;
import org.eclipse.collections.impl.factory.primitive.ObjectFloatMaps;

public class ConceptVector {
    float norm = 0;
    MutableObjectFloatMap<Integer> documentScores = ObjectFloatMaps.mutable.empty();

    public ConceptVector(int numDocs) {

    }

    public void merge(ConceptVector other) {
        for (int documentId: other.documentScores.keySet()) {
            this.addScore(documentId, other.getScore(documentId));
        }
    }

    public ConceptVector prune(int windowSize, float dropOff) {
        return this;
    }

    public float dotProduct(ConceptVector other) {
        MutableObjectFloatMap<Integer> theirScores = other.documentScores;
        float norm1 = 0;
        float norm2 = 0;
        float dotProduct = 0;
        for (int documentId: documentScores.keySet()) {
            float ourScore = documentScores.get(documentId);
            float theirScore = 0;
            if (theirScores.containsKey(documentId)) {
                theirScore = theirScores.get(documentId);
            }
            dotProduct += ourScore * theirScore;
            norm1 += ourScore * ourScore;
        }
        for (int documentId: theirScores.keySet()) {
            float theirScore = theirScores.get(documentId);
            norm2 += theirScore * theirScore;
        }
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt((norm2))));
    }

    public void sort() {
    }

    public Integer[] getSortedDocumentIds() {
        sort();
        return documentScores.keySet().toArray(new Integer[0]);
    }

    public void addScore(int document, float score) {
        if (score > 0) {
            this.documentScores.addToValue(document, score);
        }
    }

    public float getScore(int documentId) {
        return this.documentScores.get(documentId);
    }
}
