package be.vanoosten.esa.tools;

import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NarrativeVectorizer implements TextVectorizer {
    static double TOPIC_COHESION = 0.15;

    private TextVectorizer vectorizer;

    public NarrativeVectorizer(TextVectorizer vectorizer) {
        this.vectorizer = vectorizer;
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

        //We now have sentences grouped by topic and can vectorize them
        Map<String, Float> mergedWeights = new HashMap<>();
        for (NarrativeTopic topic: topics) {
            ConceptVector conceptVector = vectorizer.vectorize(String.join(" ", topic.getSentences()));
            this.mergeVectors(mergedWeights, conceptVector.conceptWeights);
        }
        return new ConceptVector(mergedWeights);
    }

    private void mergeVectors(Map<String, Float> mergedWeights, Map<String, Float> weights) {
        //Merges two vectors using addition (not good math at all)
        for(String key: weights.keySet()) {
            if (mergedWeights.containsKey(key)) {
                Float weight = mergedWeights.get(key);
                mergedWeights.put(key, weight + weights.get(key));
            } else {
                mergedWeights.put(key, weights.get(key));
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
