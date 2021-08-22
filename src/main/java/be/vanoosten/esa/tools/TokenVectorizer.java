package be.vanoosten.esa.tools;

import static be.vanoosten.esa.WikiIndexer.TEXT_FIELD;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TokenVectorizer implements TextVectorizer {
    private final TextVectorizer vectorizer;
    private final Analyzer analyzer;
    private final int maxConcepts;

    public TokenVectorizer(TextVectorizer vectorizer, Analyzer analyzer, int maxConcepts) {
        this.vectorizer = vectorizer;
        this.analyzer = analyzer;
        this.maxConcepts = maxConcepts;
    }

    public ConceptVector vectorize(String text) throws Exception {
        ArrayList<String> tokens = getTokens(text);

        Map<String, Float> mergedWeights = new HashMap<>();
        for (String token: tokens) {
            ConceptVector conceptVector = vectorizer.vectorize(token);
            this.mergeVectors(mergedWeights, conceptVector.conceptWeights);
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

    private void mergeVectors(Map<String, Float> mergedWeights, Map<String, Float> weights) {
        for(String key: weights.keySet()) {
            if (mergedWeights.containsKey(key)) {
                Float weight = mergedWeights.get(key);
                mergedWeights.put(key, weight + weights.get(key));
            } else {
                mergedWeights.put(key, weights.get(key));
            }
        }
    }

    private ArrayList<String> getTokens(String text) throws IOException {
        ArrayList<String> tokens = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, text);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            tokens.add(termAttribute.toString());
        }
        tokenStream.close();
        return tokens;
    }
}
