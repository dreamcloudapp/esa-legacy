package com.dreamcloud.esa.documentPreprocessor;

public class DocumentPreprocessorFactory {
    public DocumentPreprocessor getPreprocessor(String type) {
        switch (type) {
            case "lemma":
                return new StanfordLemmaPreprocessor();
            case "default":
            default:
                return new StandardPreprocessor();
        }
    }
}
