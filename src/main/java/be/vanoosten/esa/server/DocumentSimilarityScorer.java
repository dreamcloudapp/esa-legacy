package be.vanoosten.esa.server;

import be.vanoosten.esa.VectorizerFactory;
import be.vanoosten.esa.WikiFactory;
import be.vanoosten.esa.database.DocumentScore;
import be.vanoosten.esa.tools.SemanticSimilarityTool;

public class DocumentSimilarityScorer {
    public DocumentScore score(DocumentSimilarityRequestBody request) throws Exception {
        WikiFactory.docType = request.getDocumentType();
        VectorizerFactory vectorizerFactory = new VectorizerFactory(request.vectorizer, request.getConceptLimit(), 0);
        SemanticSimilarityTool semanticSimilarity = new SemanticSimilarityTool(vectorizerFactory.getTextVectorizer());
        double score = semanticSimilarity.findSemanticSimilarity(request.documentText1, request.documentText2);
        return new DocumentScore("success", (float) score);
    }
}
