package com.dreamcloud.esa;

import com.dreamcloud.esa.tools.LemmaVectorizer;
import com.dreamcloud.esa.tools.NarrativeVectorizer;
import com.dreamcloud.esa.tools.TextVectorizer;
import com.dreamcloud.esa.tools.Vectorizer;
import org.apache.lucene.analysis.Analyzer;
import java.io.IOException;
import java.nio.file.Paths;

public class VectorizerFactory {
    private String type;
    private int conceptLimit;
    private double cohesion;
    private AnalyzerFactory analyzerFactory;
    String documentPath;

    public VectorizerFactory(AnalyzerFactory analyzerFactory, String documentPath, String type, int conceptLimit, double cohesion) {
        this.analyzerFactory = analyzerFactory;
        this.documentPath = documentPath;
        this.type = type == null ? "" : type;
        this.conceptLimit = conceptLimit;
        this.cohesion = cohesion;
    }

    public VectorizerFactory(AnalyzerFactory analyzerFactory, String documentPath, String type, int conceptLimit) {
        this.documentPath = documentPath;
        this.analyzerFactory = analyzerFactory;
        this.type = type == null ? "" : type;
        this.conceptLimit = conceptLimit;
        this.cohesion = 0;
    }

    public TextVectorizer getTextVectorizer() throws IOException {
        Vectorizer base = new Vectorizer(Paths.get(documentPath), analyzerFactory.getAnalyzer());
        base.setConceptCount(this.conceptLimit);
        switch(this.type) {
            case "narrative":
                NarrativeVectorizer narrativeVectorizer = new NarrativeVectorizer(base, analyzerFactory.getAnalyzer(), conceptLimit);
                narrativeVectorizer.setCohesion(cohesion);
                narrativeVectorizer.setDebug(true);
                return narrativeVectorizer;
            case"default":
            default:
                return base;
        }
    }

    public TextVectorizer getLemmaVectorizer() throws IOException {
        Vectorizer base = new Vectorizer(Paths.get(documentPath), analyzerFactory.getAnalyzer());
        base.setConceptCount(this.conceptLimit);

        TextVectorizer vectorizer;

        switch(this.type) {
            case "narrative":
                NarrativeVectorizer narrativeVectorizer = new NarrativeVectorizer(base, analyzerFactory.getAnalyzer(), conceptLimit);
                narrativeVectorizer.setCohesion(cohesion);
                narrativeVectorizer.setDebug(true);
                vectorizer = narrativeVectorizer;
                break;
            case"default":
            default:
                vectorizer = base;
        }

        return new LemmaVectorizer(vectorizer);
    }
}
