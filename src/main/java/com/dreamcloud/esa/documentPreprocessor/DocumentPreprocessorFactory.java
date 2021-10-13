package com.dreamcloud.esa.documentPreprocessor;

public class DocumentPreprocessorFactory {
    protected String stanfordPosTags;

    public DocumentPreprocessor getPreprocessor(String type) {
        switch (type) {
            case "stanford-lemma":
                return new StanfordLemmaPreprocessor(stanfordPosTags);
            case "default":
            default:
                return new StandardPreprocessor();
        }
    }

    public void setStanfordPosTags(String stanfordPosTags) {
        this.stanfordPosTags = stanfordPosTags;
    }
}
