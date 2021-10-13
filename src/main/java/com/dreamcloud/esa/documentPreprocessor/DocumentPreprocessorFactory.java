package com.dreamcloud.esa.documentPreprocessor;

import com.dreamcloud.esa.EsaOptions;

public class DocumentPreprocessorFactory {
    protected String stanfordPosTags;

    public DocumentPreprocessorFactory() {

    }

    public DocumentPreprocessor getPreprocessor(String type) {
        switch (type) {
            case "stanford-lemma":
                return new StanfordLemmaPreprocessor(stanfordPosTags);
            case "wiki":
                return new WikiPreprocessor();
            case "default":
            default:
                return new StandardPreprocessor();
        }
    }

    public void setStanfordPosTags(String stanfordPosTags) {
        this.stanfordPosTags = stanfordPosTags;
    }
}
