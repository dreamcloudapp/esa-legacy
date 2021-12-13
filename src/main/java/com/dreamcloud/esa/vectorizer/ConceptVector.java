package com.dreamcloud.esa.vectorizer;

import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.*;

public class ConceptVector {
    private static HashMap<String, Float> allConceptWeights = null;
    private static HashMap<String, Integer> allIncomingLinks =  null;
    Map<String, Float> conceptWeights;

    private static void init(IndexReader reader) throws IOException {
        if (allConceptWeights == null || allIncomingLinks == null) {
            allConceptWeights = new HashMap<>();
            allIncomingLinks = new HashMap<>();
            int maxDoc = reader.maxDoc();
            for (int i=0; i<maxDoc; i++) {
                Document doc = reader.document(i);
                IndexableField concept = doc.getField("title");
                allConceptWeights.put(concept.stringValue(), 0.0f);
                IndexableField incomingLinks = doc.getField("incomingLinks");
                allIncomingLinks.put(concept.stringValue(), incomingLinks.numericValue().intValue());
            }
        }
    }

    public void initConcepts(IndexReader reader) throws IOException {
        //init(reader);
        this.conceptWeights = new HashMap<>();
        //this.conceptWeights.putAll(allConceptWeights);
    }

    ConceptVector(TopDocs td, IndexReader indexReader) throws IOException {
        initConcepts(indexReader);

        //Add initial scores and incoming link counts
        for (ScoreDoc scoreDoc : td.scoreDocs) {
            Document doc = indexReader.document(scoreDoc.doc);
            String concept = doc.get("title");
            conceptWeights.put(concept, scoreDoc.score);
        }

        //Backrub
        int topDocCount = Math.round(td.scoreDocs.length * 0.00f);
        int currentDoc = 0;
        for (ScoreDoc scoreDoc : td.scoreDocs) {
            if (++currentDoc >= topDocCount) {
                break;
            }
            Document doc = indexReader.document(scoreDoc.doc);
            String topConcept = doc.get("title");
            int topIncomingLinks = allIncomingLinks.getOrDefault(topConcept, 0);
            IndexableField[] outgoingLinks = doc.getFields("outgoingLink");
            for (IndexableField outgoingLink: outgoingLinks) {
                String concept = outgoingLink.stringValue();
                int conceptIncomingLinks = allIncomingLinks.getOrDefault(concept, 0);
                if (conceptWeights.containsKey(concept)) {
                    System.out.println("Link from '" + topConcept + "' to '" + concept + "':");
                    System.out.println("==================================================");
                    float score = conceptWeights.get(concept);
                    if (score == 0) {
                        System.out.println("0-score: latter concept must be more general");
                        System.out.println("Links: " + topIncomingLinks + ", " + conceptIncomingLinks);
                        float logDiff = (float) (Math.log10(conceptIncomingLinks) - Math.log10(topIncomingLinks));
                        System.out.println("Log difference: " + logDiff);
                        if (logDiff > 1) {
                            System.out.println("Increasing score from " + score + " to " + (0.5f * scoreDoc.score));
                            score += (0.5f * scoreDoc.score);
                        } else {
                            System.out.println("Not increasing score, diff too low");
                        }
                    } else {
                        System.out.println("Increasing score from " + score + " to " + (0.5f * scoreDoc.score));
                        score += (0.5f * scoreDoc.score);
                    }
                    System.out.println("==================================================" +
                            "\n");
                    conceptWeights.put(concept, score);
                }
            }
        }

        HashMap<String, Float> nonZeroScores = new HashMap<>();
        for ( Iterator<String> topConcepts = this.topConcepts(); topConcepts.hasNext(); ) {
            String concept = topConcepts.next();
            float score = conceptWeights.get(concept);
            if (score == 0.0f) {
                break;
            }
            nonZeroScores.put(concept, score);
        }
        conceptWeights = nonZeroScores;
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
