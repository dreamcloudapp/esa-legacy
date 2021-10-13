package com.dreamcloud.esa.documentPreprocessor;

import com.dreamcloud.esa.EsaOptions;

public class DocumentPreprocessorFactory {
    protected EsaOptions options;
    protected String stanfordPosTags;

    public DocumentPreprocessorFactory(EsaOptions options) {
        this.options = options;
    }

    public DocumentPreprocessor getPreprocessor(String type) {
        switch (type) {
            case "stanford-lemma":
                return new StanfordLemmaPreprocessor(stanfordPosTags);
            case "wiki":
                return new WikiPreprocessor(options);
            case "default":
            default:
                return new StandardPreprocessor();
        }
    }

    public void setStanfordPosTags(String stanfordPosTags) {
        this.stanfordPosTags = stanfordPosTags;
    }
}
