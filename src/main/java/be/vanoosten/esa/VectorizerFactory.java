package be.vanoosten.esa;

import be.vanoosten.esa.tools.NarrativeVectorizer;
import be.vanoosten.esa.tools.TextVectorizer;
import be.vanoosten.esa.tools.Vectorizer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;

import java.io.File;
import java.io.IOException;

public class VectorizerFactory {
    private final String type;
    private final String similarity;
    private final int conceptLimit;
    private final double cohesion;
    private final Analyzer analyzer;

    public VectorizerFactory(String type, String similarity, int conceptLimit, double cohesion) {
        this.analyzer = WikiAnalyzerFactory.getVectorizingAnalyzer();
        this.type = type;
        this.similarity = similarity;
        this.conceptLimit = conceptLimit;
        this.cohesion = cohesion;
    }

    public TextVectorizer getTextVectorizer() throws IOException {
        Vectorizer base = new Vectorizer(new File("./termdoc"), analyzer);
        base.setConceptCount(this.conceptLimit);
        base.setSimilarity(this.getSimilarity());
        switch(this.type) {
            case "narrative":
                NarrativeVectorizer narrativeVectorizer = new NarrativeVectorizer(base, analyzer, conceptLimit);
                narrativeVectorizer.setCohesion(cohesion);
                narrativeVectorizer.setDebug(true);
                return narrativeVectorizer;
            default:
                return base;
        }
    }

    private Similarity getSimilarity() {
        switch (similarity) {
            case "bm25":
                return new BM25Similarity();
            default:
                return new DefaultSimilarity();
        }
    }
}
