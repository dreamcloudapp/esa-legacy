package com.dreamcloud.esa.tools;

import com.dreamcloud.esa.vectorizer.ConceptVector;
import com.dreamcloud.esa.vectorizer.TextVectorizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import java.util.Properties;

public class LemmaVectorizer implements TextVectorizer {
    TextVectorizer vectorizer;
    static StanfordCoreNLP stanfordPipeline;

    public LemmaVectorizer(TextVectorizer vectorizer) {
        this.vectorizer = vectorizer;
    }

    //takes a while
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

    public ConceptVector vectorize(String text) throws Exception {
        //Get lemmas for the text
        StanfordCoreNLP pipeline = getStanfordPipeline();
        CoreDocument document = pipeline.processToCoreDocument(text);
        StringBuilder lemmatizedText = new StringBuilder();
        for (CoreLabel token: document.tokens()) {
            lemmatizedText.append(token.lemma()).append(" ");
        }
        //Pass along to our vectorizer
        return this.vectorizer.vectorize(lemmatizedText.toString());
    }
}
