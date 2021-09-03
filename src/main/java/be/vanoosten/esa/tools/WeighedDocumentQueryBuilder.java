package be.vanoosten.esa.tools;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.ArrayList;

import static be.vanoosten.esa.WikiIndexer.TEXT_FIELD;

public class WeighedDocumentQueryBuilder {
    Analyzer analyzer;
    IndexSearcher searcher;

    public WeighedDocumentQueryBuilder(Analyzer analyzer, IndexSearcher searcher) {
        this.analyzer = analyzer;
        this.searcher = searcher;
    }

    /**
     *
     * @param documentId Term for the document ID to use for computing relevance
     * @param documentText string of the document text used as the query
     * @return a new query text with weights added ("cat dog" --> "cat^1.1 dog^0.9")
     */
    public String weight(Term documentId,  String documentText) throws IOException {
        DocumentTermRelevance relevance = new DocumentTermRelevance(documentId, searcher);
        ArrayList<Term> terms = this.getTerms(documentText);
        StringBuilder queryTextBuilder = new StringBuilder();
        for(Term term: terms) {
            queryTextBuilder.append(term.text()).append("^").append(relevance.score(term)).append(" ");
        }
        return queryTextBuilder.toString().trim();
    }

    private ArrayList<Term> getTerms(String documentText) throws IOException {
        ArrayList<Term> tokens = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, documentText);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            tokens.add(new Term(TEXT_FIELD, termAttribute.toString()));
        }
        tokenStream.close();
        return tokens;
    }
}
