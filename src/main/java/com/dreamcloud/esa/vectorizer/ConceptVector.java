package com.dreamcloud.esa.vectorizer;

import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.*;

public class ConceptVector {
    Map<String, Float> conceptWeights;

    ConceptVector(TopDocs td, IndexReader indexReader) throws IOException {
        Map<String, Integer> linkCounts = new HashMap<>();
        for (ScoreDoc linkDoc : td.scoreDocs) {
            Document doc = indexReader.document(linkDoc.doc);
            IndexableField[] linkFields = doc.getFields("outgoingLink");
            for (IndexableField linkField: linkFields) {
                String link = linkField.stringValue();
                linkCounts.put(link, linkCounts.getOrDefault(link, 0) + 1);
            }
        }

        conceptWeights = new HashMap<>();
        for (ScoreDoc scoreDoc : td.scoreDocs) {
            String concept = indexReader.document(scoreDoc.doc).get("title");
            float baseScore = scoreDoc.score;
            /*float backRubScore = (float) Math.log(1 + linkCounts.getOrDefault(concept, 0));
            float backRubScore = (float) Math.log(1 + linkCounts.getOrDefault(concept, 0));
            if (concept.startsWith("category")) {
                System.out.println("Found a category page!");
                backRubScore += 5;
            }*/
            conceptWeights.put(concept, baseScore /*+ backRubScore*/);
        }

        /*int i = 0;
        Map<String, Float> cutoffWeights = new HashMap<>();
        for(Iterator<String> it = this.topConcepts(); it.hasNext() && i++ < 450;) {
            String concept = it.next();
            cutoffWeights.put(concept, conceptWeights.get(concept));
        }
        conceptWeights = cutoffWeights;*/
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

    public ConceptVector prune(int windowSize, float dropOff) {
        return this;
        /*if (this.conceptWeights.size() == 0) {
            return this;
        }

        ConceptVector pruned = new ConceptVector();

        int w = 0;
        float topScore = -1;
        float firstScore = -1;
        for (Iterator<String> it = this.topConcepts(); it.hasNext(); w++) {
            String concept = it.next();
            float score = this.conceptWeights.get(concept);
            pruned.conceptWeights.put(concept, score);

            if (topScore == -1) {
                topScore = score;
            } /*else {
                if (score < topScore * dropOff) {
                    break;
                }
            }*/
            /*if (w == 0) {
                firstScore = score;
            }

            if (w == windowSize) {
                //Process the window
                if (firstScore - score > dropOff * topScore) {
                    break;
                }
                w = -1;
            }
        }
        return pruned;*/
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
