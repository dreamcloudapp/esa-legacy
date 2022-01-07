package com.dreamcloud.esa.vectorizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dreamcloud.esa.EsaOptions;
import com.dreamcloud.esa.similarity.SimilarityFactory;
import com.dreamcloud.esa.similarity.TrueTFIDFSimilarity;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Can present text as a vector of weighted concepts.
 *
 * @author Philip van Oosten
 */
public class Vectorizer implements AutoCloseable, TextVectorizer {
    EsaOptions options;
    Directory termToConceptDirectory;
    IndexReader indexReader;
    IndexSearcher searcher;
    QueryParser queryParser;
    protected Map<String, ConceptVector> conceptVectorCache;

    public Vectorizer(EsaOptions options) throws IOException {
        this.options = options;
        termToConceptDirectory = FSDirectory.open(options.indexPath);
        indexReader = DirectoryReader.open(termToConceptDirectory);
        searcher = new IndexSearcher(indexReader);
        searcher.setSimilarity(SimilarityFactory.getSimilarity());
        queryParser = new QueryParser("text", options.analyzer);
        conceptVectorCache = new HashMap<>();
    }

    public ConceptVector vectorize(String text) throws Exception {
        if (options.preprocessor != null) {
            text = options.preprocessor.process(text);
        }

        if (!this.conceptVectorCache.containsKey(text)) {
            Query query = queryParser.parse(text);
            //Query query = FunctionScoreQuery.boostByValue(query, DoubleValuesSource.fromDoubleField("boost"));
            int maxDocs = options.documentLimit > 0 ? options.documentLimit : indexReader.numDocs();
            TopScoreDocCollector collector = TopScoreDocCollector.create(maxDocs, maxDocs);
            searcher.search(query, collector);
            TopDocs td = collector.topDocs();
            //this.conceptVectorCache.put(text, new ConceptVector(td, indexReader));
        }
        return this.conceptVectorCache.get(text);
    }

    public void close() {
        try {
            indexReader.close();
            termToConceptDirectory.close();
        } catch (IOException ex) {
            Logger.getLogger(Vectorizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
