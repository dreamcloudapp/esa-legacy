package be.vanoosten.esa;

import be.vanoosten.esa.tools.NarrativeVectorizer;
import be.vanoosten.esa.tools.TextVectorizer;
import be.vanoosten.esa.tools.Vectorizer;
import org.apache.lucene.analysis.Analyzer;
import java.io.IOException;
import java.nio.file.Paths;

public class VectorizerFactory {
    private final String type;
    private final int conceptLimit;
    private final double cohesion;
    private final Analyzer analyzer;

    public VectorizerFactory(String type, int conceptLimit, double cohesion) {
        this.analyzer = AnalyzerFactory.getVectorizingAnalyzer();
        this.type = type == null ? "" : type;
        this.conceptLimit = conceptLimit;
        this.cohesion = cohesion;
    }

    public TextVectorizer getTextVectorizer() throws IOException {
        Vectorizer base = new Vectorizer(Paths.get("./index/" + WikiFactory.docType + "_termdoc"), analyzer);
        base.setConceptCount(this.conceptLimit);
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
}
