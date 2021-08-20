package be.vanoosten.esa.tools;

import java.util.ArrayList;

public class NarrativeTopic {
    ArrayList<String> sentences = new ArrayList<>();
    NarrativeTopic(String sentence) {
        this.addSentence(sentence);
    }

    public void addSentence(String sentence) {
        sentences.add(sentence);
    }

    public ArrayList<String> getSentences() {
        return sentences;
    }
}
