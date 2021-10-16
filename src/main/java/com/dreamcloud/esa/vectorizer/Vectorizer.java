package com.dreamcloud.esa.vectorizer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.dreamcloud.esa.EsaOptions;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
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

    public Vectorizer(EsaOptions options) throws IOException {
        this.options = options;
        termToConceptDirectory = FSDirectory.open(options.indexPath);
        indexReader = DirectoryReader.open(termToConceptDirectory);
        searcher = new IndexSearcher(indexReader);
        queryParser = new QueryParser("text", options.analyzer);
    }

    public ConceptVector vectorize(String text) throws Exception {
        if (options.preprocessor != null) {
            text = options.preprocessor.process(text);
        }

        Query query = queryParser.parse(text);
        TopDocs td = searcher.search(query, options.documentLimit);
        return new ConceptVector(td, indexReader);
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
