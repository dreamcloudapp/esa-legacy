package com.dreamcloud.esa.server;

import com.dreamcloud.esa.VectorizerFactory;
import com.dreamcloud.esa.WikiFactory;
import com.dreamcloud.esa.database.DocumentScore;
import com.dreamcloud.esa.tools.SemanticSimilarityTool;

public class DocumentSimilarityScorer {
    public DocumentScore score(DocumentSimilarityRequestBody request) throws Exception {
        WikiFactory.docType = request.getDocumentType();
        VectorizerFactory vectorizerFactory = new VectorizerFactory(request.vectorizer, request.getConceptLimit(), 0);
        SemanticSimilarityTool semanticSimilarity = new SemanticSimilarityTool(vectorizerFactory.getLemmaVectorizer());
        double score = semanticSimilarity.findSemanticSimilarity(request.documentText1, request.documentText2);
        return new DocumentScore("success", (float) score);
    }
}
