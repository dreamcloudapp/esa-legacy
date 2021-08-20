package be.vanoosten.esa.tools;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NarrativeVectorizer implements TextVectorizer {
    static double TOPIC_COHESION = 0.15;

    private final TextVectorizer vectorizer;
    private final int maxConcepts;

    public NarrativeVectorizer(TextVectorizer vectorizer, int maxConcepts) {
        this.vectorizer = vectorizer;
        this.maxConcepts = maxConcepts;
    }

    public ConceptVector vectorize(String text) throws Exception {
        ArrayList<NarrativeTopic> topics = new ArrayList<>();
        ArrayList<String> sentences = getSentences(text);
        System.out.println("Checking cohesion for " + sentences.size() + " sentences...");
        for (String sentence: sentences) {
            //Compare sentence to existing narrative topics
            boolean hasCohesion = false;
            for (NarrativeTopic topic : topics) {
                if (this.hasTopicalCohesion(sentence, topic)) {
                    topic.addSentence(sentence);
                    hasCohesion = true;
                    System.out.println("Found cohesion.");
                    break;
                }
            }
            if (!hasCohesion) {
                topics.add(new NarrativeTopic(sentence));
            }
        }

        //We now have sentences grouped by topic and can vectorize them
        Map<String, Float> mergedWeights = new HashMap<>();
        for (NarrativeTopic topic: topics) {
            ConceptVector conceptVector = vectorizer.vectorize(String.join(" ", topic.getSentences()));
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

    private void mergeVectors(Map<String, Float> mergedWeights, Map<String, Float> weights, int salience) {
        double salienceLog = Math.log(salience) + 1;
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
        ConceptVector formerVector = vectorizer.vectorize(sentence);
        ConceptVector latterVector = vectorizer.vectorize(topicText);
        return formerVector.dotProduct(latterVector) >= TOPIC_COHESION;
    }

    private ArrayList<String> getSentences(String text) {
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
