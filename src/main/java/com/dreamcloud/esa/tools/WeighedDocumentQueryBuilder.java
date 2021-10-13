package com.dreamcloud.esa.tools;

import com.dreamcloud.esa.indexer.WikiIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        System.out.println("weighing dream " + documentId.text());
        System.out.println("==================================================");
        DocumentTermRelevance relevance = new DocumentTermRelevance(documentId, searcher);
        ArrayList<Term> terms = this.getTerms(documentText);
        StringBuilder queryTextBuilder = new StringBuilder();
        Map<String, Integer> termOccurrences = new HashMap<>();
        for(Term term: terms) {
            if (!termOccurrences.containsKey(term.text())) {
                termOccurrences.put(term.text(), 1);
            } else {
                int count = termOccurrences.get(term.text());
                termOccurrences.put(term.text(), ++count);
            }
        }
        for(Term term: terms) {
            float score = relevance.score(term);
            score = (float) Math.pow(score, 1.0 / termOccurrences.get(term.text()));
            queryTextBuilder.append(term.text()).append("^").append(score).append(" ");
            System.out.println(term.text() + "\t" + score);
        }
        System.out.println("==================================================");
        return queryTextBuilder.toString().trim();
    }

    private ArrayList<Term> getTerms(String documentText) throws IOException {
        ArrayList<Term> tokens = new ArrayList<>();
        TokenStream tokenStream = analyzer.tokenStream(WikiIndexer.TEXT_FIELD, documentText);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            //System.out.println("dd-token: " + termAttribute.toString());
            tokens.add(new Term(WikiIndexer.TEXT_FIELD, termAttribute.toString()));
        }
        tokenStream.close();
        return tokens;
    }
}
