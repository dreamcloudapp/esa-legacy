package com.dreamcloud.esa.vectorizer;

import com.dreamcloud.esa.analyzer.EsaAnalyzer;
import com.dreamcloud.esa.database.InverseTermMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SqlVectorizer implements TextVectorizer {
    protected Analyzer analyzer;
    protected Map<String, ConceptVector> conceptVectorCache;

    public SqlVectorizer(Analyzer analyzer) {
        this.analyzer = analyzer;
        this.conceptVectorCache = new HashMap<>();
    }

    public ConceptVector vectorize(String text) throws Exception {
        if (!this.conceptVectorCache.containsKey(text)) {
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
            this.conceptVectorCache.put(text, this.getTop450(vector));
        }
        return this.conceptVectorCache.get(text);
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
