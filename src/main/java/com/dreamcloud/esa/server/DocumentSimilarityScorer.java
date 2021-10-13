package com.dreamcloud.esa.server;

import com.dreamcloud.esa.database.DocumentScore;
import com.dreamcloud.esa.tools.SemanticSimilarityTool;
import com.dreamcloud.esa.vectorizer.TextVectorizer;

public class DocumentSimilarityScorer {
    TextVectorizer vectorizer;

    //todo: fix API stuff so it's all nice and separate from command line
    public DocumentSimilarityScorer(TextVectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    public DocumentScore score(DocumentSimilarityRequestBody request) throws Exception {
        SemanticSimilarityTool semanticSimilarity = new SemanticSimilarityTool(vectorizer);
        double score = semanticSimilarity.findSemanticSimilarity(request.documentText1, request.documentText2);
        return new DocumentScore("success", (float) score);
    }
}
