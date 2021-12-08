package com.dreamcloud.esa.vectorizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

public class ConceptVector {
    Map<String, Float> conceptWeights;

    ConceptVector(TopDocs td, IndexReader indexReader) throws IOException {
        conceptWeights = new HashMap<>();
        for (ScoreDoc scoreDoc : td.scoreDocs) {
            String concept = indexReader.document(scoreDoc.doc).get("title");
            conceptWeights.put(concept, scoreDoc.score);
        }
    }

    public ConceptVector(Map<String, Float> conceptWeights) {
        this.conceptWeights = conceptWeights;
    }

    public ConceptVector() {
        this.conceptWeights = new HashMap<>();
    }

    public void merge(ConceptVector other) {
        for (Map.Entry<String, Float> weightEntry: other.getConceptWeights().entrySet()) {
            String concept = weightEntry.getKey();
            float score = weightEntry.getValue();

            if (this.conceptWeights.containsKey(concept)) {
                this.conceptWeights.put(concept, this.conceptWeights.get(concept) + score);
            } else {
                this.conceptWeights.put(concept, score);
            }
        }
    }

    public float dotProduct(ConceptVector other) {
        Set<String> commonConcepts = new HashSet<>(other.conceptWeights.keySet());
        commonConcepts.retainAll(conceptWeights.keySet());
        float norm1 = 0;
        float norm2 = 0;
        float dotProd = 0;
        for (String concept : commonConcepts) {
            Float w1 =  conceptWeights.get(concept);
            Float w2 =  other.conceptWeights.get(concept);
            dotProd += w1 * w2;
        }

        for (String concept : conceptWeights.keySet()) {
            float norm = conceptWeights.get(concept);
            norm1 += norm * norm;
        }

        for (String concept : other.conceptWeights.keySet()) {
            float norm = other.conceptWeights.get(concept);
            norm2 += norm * norm;
        }

        return (float) (dotProd / (Math.sqrt(norm1) * Math.sqrt((norm2))));
    }

    public Iterator<String> topConcepts() {
        return conceptWeights.entrySet().stream().
                sorted((Map.Entry<String, Float> e1, Map.Entry<String, Float> e2) -> (int) Math.signum(e2.getValue() - e1.getValue())).
                map(e -> e.getKey()).
                iterator();
    }

    public Map<String, Float> getConceptWeights() {
        return conceptWeights;
    }
}
