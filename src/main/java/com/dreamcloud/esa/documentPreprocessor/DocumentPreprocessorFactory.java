package com.dreamcloud.esa.documentPreprocessor;

import java.util.List;

public class DocumentPreprocessorFactory {
    protected List<String> stanfordPosTags;

    public DocumentPreprocessorFactory() {

    }

    public DocumentPreprocessor getPreprocessor(String type) {
        switch (type) {
            case "stanford-lemma":
                return new StanfordLemmaPreprocessor(stanfordPosTags);
            case "wiki":
                return new WikiPreprocessor();
            case "standard":
                return new StandardPreprocessor();
            default:
                return new NullPreprocessor();
        }
    }

    public void setStanfordPosTags(List<String> stanfordPosTags) {
        this.stanfordPosTags = stanfordPosTags;
    }
}
