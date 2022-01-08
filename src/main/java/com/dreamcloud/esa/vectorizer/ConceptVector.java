package com.dreamcloud.esa.vectorizer;

import org.apache.commons.collections.ComparatorUtils;

import java.util.*;

public class ConceptVector {
    float[] documentScores = null;

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
        /*ConceptVector pruned = new ConceptVector();
        if (conceptWeights.size() == 0) {
            return pruned;
        }

        Integer[] orderedConcepts = conceptWeights.keySet().toArray(Integer[]::new);
        for (int conceptIdx = 0; conceptIdx < orderedConcepts.length; conceptIdx++) {
            int windowHeadConcept = orderedConcepts[conceptIdx];
            float windowHeadScore = this.conceptWeights.get(windowHeadConcept);
            pruned.conceptWeights.put(windowHeadConcept, windowHeadScore);

            if (conceptIdx + windowSize < orderedConcepts.length) {
                int windowTailConcept = orderedConcepts[conceptIdx + windowSize];
                float windowTailScore = this.conceptWeights.get(windowTailConcept);
                if ((windowHeadScore - windowTailScore) < windowHeadScore * dropOff) {
                    break;
                }
            }
        }

        return pruned;*/
        return this;
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

    /*
    public float dotProduct(ConceptVector other) {
        Set<Integer> commonConcepts = new HashSet<>(other.conceptWeights.keySet());
        commonConcepts.retainAll(conceptWeights.keySet());
        float norm1 = 0;
        float norm2 = 0;
        float dotProd = 0;
        for (int concept : commonConcepts) {
            Float w1 =  conceptWeights.get(concept);
            Float w2 =  other.conceptWeights.get(concept);
            dotProd += w1 * w2;
        }

        for (int concept : conceptWeights.keySet()) {
            float norm = conceptWeights.get(concept);
            norm1 += norm * norm;
        }

        for (int concept : other.conceptWeights.keySet()) {
            float norm = other.conceptWeights.get(concept);
            norm2 += norm * norm;
        }

        return (float) (dotProd / (Math.sqrt(norm1) * Math.sqrt((norm2))));
    }*/

    public Iterator<Integer> topConcepts() {
        return null;
        /*return conceptWeights.entrySet().stream().
                sorted((Map.Entry<Integer, Float> e1, Map.Entry<Integer, Float> e2) -> (int) Math.signum(e2.getValue() - e1.getValue())).
                map(Map.Entry::getKey).
                iterator();*/
    }

    public Map<Integer, Float> getConceptWeights() {
        return null;
        //return conceptWeights;
    }

    public ConceptVector pruneToSize(int n) {
        Integer[] documentIds = new Integer[documentScores.length];
        for (int documentId = 0; documentId < documentIds.length; documentId++) {
            documentIds[documentId] = documentId;
        }
        Arrays.sort(documentIds, (o1, o2) -> Float.compare(documentScores[o2], documentScores[o1]));

        for (int documentId = n; documentId < documentIds.length; documentId++) {
            documentScores[documentIds[documentId]] = 0;
        }

        /*ConceptVector vector = new ConceptVector();
        int i = 0;
        for ( Iterator<Integer> topConcepts = this.topConcepts(); topConcepts.hasNext(); ) {
            int concept = topConcepts.next();
            float score = conceptWeights.get(concept);
            vector.conceptWeights.put(concept, score);
            if (i++ == n) {
                break;
            }
        }
        return vector;*/
        return this;
    }

    public void addScore(int document, float score) {
        this.documentScores[document - 1] += score;
    }
}
