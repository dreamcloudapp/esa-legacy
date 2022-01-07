package com.dreamcloud.esa.vectorizer;

import java.util.*;

public class ConceptVector {
    Map<Integer, Float> conceptWeights;

    public ConceptVector(Map<Integer, Float> conceptWeights) {
        this.conceptWeights = conceptWeights;
    }

    public ConceptVector() {
        this.conceptWeights = new HashMap<>();
    }

    public void merge(ConceptVector other) {
        for (Map.Entry<Integer, Float> weightEntry: other.getConceptWeights().entrySet()) {
            int concept = weightEntry.getKey();
            float score = weightEntry.getValue();

            if (this.conceptWeights.containsKey(concept)) {
                this.conceptWeights.put(concept, this.conceptWeights.get(concept) + score);
            } else {
                this.conceptWeights.put(concept, score);
            }
        }
    }

    public ConceptVector prune(int windowSize, float dropOff) {
        ConceptVector pruned = new ConceptVector();
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

        return pruned;
    }

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
    }

    public Iterator<Integer> topConcepts() {
        return conceptWeights.entrySet().stream().
                sorted((Map.Entry<Integer, Float> e1, Map.Entry<Integer, Float> e2) -> (int) Math.signum(e2.getValue() - e1.getValue())).
                map(Map.Entry::getKey).
                iterator();
    }

    public Map<Integer, Float> getConceptWeights() {
        return conceptWeights;
    }

    public ConceptVector pruneToSize(int n) {
        ConceptVector vector = new ConceptVector();
        int i = 0;
        for ( Iterator<Integer> topConcepts = this.topConcepts(); topConcepts.hasNext(); ) {
            int concept = topConcepts.next();
            float score = conceptWeights.get(concept);
            vector.conceptWeights.put(concept, score);
            if (i++ == n) {
                break;
            }
        }
        return vector;
    }
}
