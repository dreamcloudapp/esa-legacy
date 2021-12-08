package com.dreamcloud.esa.vectorizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dreamcloud.esa.EsaOptions;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
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
        //searcher.setSimilarity(new TrueTFIDFSimilarity());
        queryParser = new QueryParser("text", options.analyzer);
        conceptVectorCache = new HashMap<>();
    }

    public ConceptVector vectorize(String text) throws Exception {
        if (!this.conceptVectorCache.containsKey(text)) {
            if (options.preprocessor != null) {
                text = options.preprocessor.process(text);
            }

            Query query = queryParser.parse(text);
            //TopScoreDocCollector collector = TopScoreDocCollector.create(indexReader.numDocs(), indexReader.numDocs());
            //searcher.search(query, collector);
            TopDocs td = searcher.search(query, options.documentLimit);
            //TopDocs td = collector.topDocs();
            this.conceptVectorCache.put(text, new ConceptVector(td, indexReader));
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
