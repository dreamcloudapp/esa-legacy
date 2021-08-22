package be.vanoosten.esa.tools;

import org.apache.lucene.analysis.Analyzer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NarrativeVectorizer implements TextVectorizer {
    private final Analyzer analyzer;
    private double TOPIC_COHESION = 0.175;

    private final TextVectorizer vectorizer;
    private final int maxConcepts;
    private boolean debug = false;
    private HashMap<String, ConceptVector> vectorCache = new HashMap<>();

    public NarrativeVectorizer(TextVectorizer vectorizer, Analyzer analyzer, int maxConcepts) {
        this.vectorizer = vectorizer;
        this.maxConcepts = maxConcepts;
        this.analyzer = analyzer;
    }

    public void setCohesion(double cohesion) {
        this.TOPIC_COHESION = cohesion;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public ConceptVector vectorize(String text) throws Exception {
        ArrayList<NarrativeTopic> topics = new ArrayList<>();
        ArrayList<String> sentences = getSentences(text);
        for (String sentence: sentences) {
            //Compare sentence to existing narrative topics
            boolean hasCohesion = false;
            for (NarrativeTopic topic : topics) {
                if (this.hasTopicalCohesion(sentence, topic)) {
                    topic.addSentence(sentence);
                    hasCohesion = true;
                    break;
                }
            }
            if (!hasCohesion) {
                topics.add(new NarrativeTopic(sentence));
            }
        }

        //Debug mode: display the topic-sentence groupings
        if (this.debug) {
            System.out.println("Topic Breakdown");
            System.out.println("----------------------------------------");
            for (NarrativeTopic topic: topics) {
                for (String sentence: topic.getSentences()) {
                    System.out.println(sentence);
                }
                System.out.println("----------------------------------------");;
            }
            System.out.println("");
        }


        //We now have sentences grouped by topic and can vectorize them
        Map<String, Float> mergedWeights = new HashMap<>();
        for (NarrativeTopic topic: topics) {
            ConceptVector conceptVector = vectorizeText(String.join(" ", topic.getSentences()));
            this.mergeVectors(mergedWeights, conceptVector.conceptWeights, topic.getSentences().size());
        }
        ConceptVector mergedConcepts = new ConceptVector(mergedWeights);

        //Can only include N number of concepts so vectors can be compared
        Map<String, Float> topWeights = new HashMap<>();
        Iterator<String> topConcepts = mergedConcepts.topConcepts();
        int i = 0;
        for (Iterator<String> it = topConcepts; it.hasNext() && i<this.maxConcepts;  i++ )      {
            String concept = it.next();
            topWeights.put(concept, mergedWeights.get(concept));
        }
        return new ConceptVector(topWeights);
    }

    private ConceptVector vectorizeText(String text) throws Exception {
        if (!vectorCache.containsKey(text)) {
            vectorCache.put(text, vectorizer.vectorize(text));
        }
        return vectorCache.get(text);
    }

    private void mergeVectors(Map<String, Float> mergedWeights, Map<String, Float> weights, int salience) {
        double salienceLog = Math.pow(Math.log(salience) + 1, 3);
        //Merges two vectors using addition (not good math at all)
        for(String key: weights.keySet()) {
            if (mergedWeights.containsKey(key)) {
                Float weight = mergedWeights.get(key);
                mergedWeights.put(key, (float) (weight + weights.get(key) * salienceLog));
            } else {
                mergedWeights.put(key, (float) (weights.get(key) * salienceLog));
            }
        }
    }

    private boolean hasTopicalCohesion(String sentence, NarrativeTopic topic) throws Exception {
        String topicText = String.join(" ", topic.getSentences());
        ConceptVector formerVector = vectorizeText(sentence);
        ConceptVector latterVector = vectorizeText(topicText);
        return formerVector.dotProduct(latterVector) >= TOPIC_COHESION;
    }

    private ArrayList<String> getSentences(String text) throws IOException {
        ArrayList<String> sentences = new ArrayList<>();
        String[] splitSentences = text.split("\\r?\\n");
        for (String sentence: splitSentences) {
            sentence = sentence.trim();
            if (!"".equals(sentence)) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }
}
