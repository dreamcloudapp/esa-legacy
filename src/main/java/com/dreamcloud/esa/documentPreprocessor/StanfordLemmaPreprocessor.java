package com.dreamcloud.esa.documentPreprocessor;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.List;
import java.util.Properties;

public class StanfordLemmaPreprocessor implements DocumentPreprocessor {
    static StanfordCoreNLP stanfordPipeline;
    List<String> stanfordPosTags;

    public StanfordLemmaPreprocessor() { }

    public StanfordLemmaPreprocessor(List<String> stanfordPosTags) {
        this.stanfordPosTags = stanfordPosTags;
    }

    static StanfordCoreNLP getStanfordPipeline() {
        if (stanfordPipeline == null) {
            Properties props = new Properties();
            // set the list of annotators to run
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
            // build pipeline
            stanfordPipeline = new StanfordCoreNLP(props);
        }
        return stanfordPipeline;
    }

    public String process(String text) {
        StanfordCoreNLP pipeline = getStanfordPipeline();
        CoreDocument document = pipeline.processToCoreDocument(text);
        StringBuilder lemmatizedText = new StringBuilder();
        for (CoreLabel token: document.tokens()) {
            if (stanfordPosTags.size() > 0) {
                boolean foundPos = false;
                for(String stanfordPosTag: stanfordPosTags) {
                    if (token.tag().startsWith(stanfordPosTag)) {
                        foundPos = true;
                        break;
                    }
                }
                if (!foundPos) {
                    continue;
                }
            }
            lemmatizedText.append(token.lemma()).append(" ");
        }
        return lemmatizedText.toString();
    }
}
