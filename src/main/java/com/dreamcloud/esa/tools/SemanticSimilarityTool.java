package com.dreamcloud.esa.tools;

/**
 * Calculates a numeric value for the semantic similarity between two texts.
 * @author Philip van Oosten
 */
public class SemanticSimilarityTool {

    TextVectorizer vectorizer;
    
    public SemanticSimilarityTool(TextVectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    public TextVectorizer getVectorizer() {
        return vectorizer;
    }

    public float findSemanticSimilarity(String formerText, String latterText) throws Exception {
        ConceptVector formerVector = vectorizer.vectorize(formerText);
        ConceptVector latterVector = vectorizer.vectorize(latterText);
        return formerVector.dotProduct(latterVector);
    }
    
}
