package com.dreamcloud.esa.server;

import com.dreamcloud.esa.DocumentType;

public class DocumentSimilarityRequestBody {
    public String documentText1;
    public String documentText2;
    public DocumentType documentType = DocumentType.WIKI;
    public String vectorizer = "default";
    public int conceptLimit = 100;

    public DocumentType getDocumentType() {
        return documentType != null ? documentType : DocumentType.WIKI;
    }

    public String getVectorizer() {
        return vectorizer != null ? vectorizer : "default";
    }

    public int getConceptLimit() {
        return conceptLimit > 0 ? conceptLimit : 100;
    }
}
