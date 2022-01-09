package com.dreamcloud.esa.vectorizer;

import java.util.*;

public class ConceptVector {
    float[] documentScores;

    public float[] getDocumentScores() {
        return documentScores;
    }

    public ConceptVector(float[] documentScores) {
        this.documentScores = documentScores;
    }

    public ConceptVector(int numDocs) {
        this.documentScores = new float[numDocs];
    }

    public void merge(ConceptVector other) {
        for (int documentId = 0; documentId < other.getDocumentScores().length; documentId++) {
            this.documentScores[documentId] += other.getDocumentScores()[documentId];
        }
    }

    public ConceptVector prune(int windowSize, float dropOff) {
        ConceptVector pruned = new ConceptVector(this.documentScores.length);

        Integer[] orderedDocumentIds = getSortedDocumentIds();
        for (int documentIdx = 0; documentIdx < orderedDocumentIds.length; documentIdx++) {
            int windowHeadId = orderedDocumentIds[documentIdx];
            float windowHeadScore = getScore(windowHeadId);
            pruned.addScore(windowHeadId, windowHeadScore);

            if (documentIdx + windowSize < orderedDocumentIds.length) {
                int windowTailId = orderedDocumentIds[documentIdx + windowSize];
                float windowTailScore = getScore(windowTailId);
                if ((windowHeadScore - windowTailScore) < windowHeadScore * dropOff) {
                    break;
                }
            }
        }
        return pruned;
    }

    public float dotProduct(ConceptVector other) {
        float[] theirScores = other.getDocumentScores();
        float norm1 = 0;
        float norm2 = 0;
        float dotProduct = 0;
        for (int documentId = 0; documentId < documentScores.length; documentId++) {
            float ourScore = documentScores[documentId];
            float theirScore = theirScores[documentId];
            dotProduct += ourScore * theirScore;
            norm1 += ourScore * ourScore;
            norm2 += theirScore * theirScore;
        }
        return (float) (dotProduct / (Math.sqrt(norm1) * Math.sqrt((norm2))));
    }

    public Integer[] getSortedDocumentIds() {
        Integer[] documentIds = new Integer[documentScores.length];
        for (int documentId = 0; documentId < documentIds.length; documentId++) {
            documentIds[documentId] = documentId;
        }
        Arrays.sort(documentIds, (o1, o2) -> Float.compare(documentScores[o2], documentScores[o1]));
        return documentIds;
    }

    public ConceptVector pruneToSize(int n) {
        Integer[] documentIds = getSortedDocumentIds();
        for (int documentId = n; documentId < documentIds.length; documentId++) {
            documentScores[documentIds[documentId]] = 0;
        }
        return this;
    }

    public void addScore(int document, float score) {
        this.documentScores[document - 1] += score;
    }

    public float getScore(int documentId) {
        return this.documentScores[documentId];
    }
}
