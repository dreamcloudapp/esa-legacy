package com.dreamcloud.esa.tools;

import com.dreamcloud.esa.vectorizer.ConceptVector;
import com.dreamcloud.esa.vectorizer.PruneOptions;
import com.dreamcloud.esa.vectorizer.TextVectorizer;

/**
 * Calculates a numeric value for the semantic similarity between two texts.
 * @author Philip van Oosten
 */
public class SemanticSimilarityTool {
    TextVectorizer vectorizer;
    PruneOptions options;
    
    public SemanticSimilarityTool(TextVectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    public SemanticSimilarityTool(TextVectorizer vectorizer, PruneOptions options) {
        this.vectorizer = vectorizer;
        this.options = options;
    }

    public TextVectorizer getVectorizer() {
        return vectorizer;
    }

    private float findSemanticSimilarity(String formerText, String latterText, PruneOptions options) throws Exception {
        ConceptVector formerVector = vectorizer.vectorize(formerText).prune(options.windowSize, options.dropOff);
        ConceptVector latterVector = vectorizer.vectorize(latterText).prune(options.windowSize, options.dropOff);
        return formerVector.dotProduct(latterVector);
    }

    public float findSemanticSimilarity(String formerText, String latterText) throws Exception {
        if (this.options != null) {
            return this.findSemanticSimilarity(formerText, latterText, this.options);
        }
        ConceptVector formerVector = vectorizer.vectorize(formerText);
        ConceptVector latterVector = vectorizer.vectorize(latterText);
        return formerVector.dotProduct(latterVector);
    }
}
