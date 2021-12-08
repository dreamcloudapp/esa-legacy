package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.analyzer.EsaAnalyzer;
import com.dreamcloud.esa.database.InverseTermMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.util.Iterator;

public class SqlVectorizer implements TextVectorizer {
    protected Analyzer analyzer;

    public SqlVectorizer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public ConceptVector vectorize(String text) throws Exception {
        InverseTermMap termMap = new InverseTermMap();
        ConceptVector vector = new ConceptVector();
        TokenStream tokenStream = analyzer.tokenStream("text", text);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            ConceptVector termVector = termMap.getTermVector(termAttribute.toString());
            vector.merge(termVector);
        }
        tokenStream.close();
        return this.getTop450(vector);
    }

    protected ConceptVector getTop450(ConceptVector vector) {
        ConceptVector top = new ConceptVector();

        int i = 0;
        for (Iterator<String> it = vector.topConcepts(); it.hasNext() && i < 450; i++) {
            String concept = it.next();
            top.conceptWeights.put(concept, vector.conceptWeights.get(concept));
        }

        return top;
    }
}
