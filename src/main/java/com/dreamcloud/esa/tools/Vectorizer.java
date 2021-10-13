package com.dreamcloud.esa.tools;

import static com.dreamcloud.esa.indexer.WikiIndexer.TEXT_FIELD;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Can present text as a vector of weighted concepts.
 *
 * @author Philip van Oosten
 */
public class Vectorizer implements AutoCloseable, TextVectorizer {

    Directory termToConceptDirectory;
    IndexReader indexReader;
    IndexSearcher searcher;
    QueryParser queryParser;
    Similarity similarity;
    int conceptCount = 1000;

    /**
     * Creates a new Vectorizer
     *
     * @param termConceptDirectory The directory where to find the indices
     * @param analyzer The analyzer to use to create search queries
     * @throws java.io.IOException
     */
    public Vectorizer(Path termConceptDirectory, Analyzer analyzer) throws IOException {
        termToConceptDirectory = FSDirectory.open(termConceptDirectory);
        indexReader = DirectoryReader.open(termToConceptDirectory);
        searcher = new IndexSearcher(indexReader);
        queryParser = new QueryParser(TEXT_FIELD, analyzer);
    }

    public ConceptVector vectorize(String text) throws ParseException, IOException {
        //We need to escape this thing!
        text = text.replaceAll("[+\\-&|!(){}\\[\\]^\"~*?:/\\\\]+", " ");
        Query query = queryParser.parse(text);
        TopDocs td = searcher.search(query, conceptCount);
        return new ConceptVector(td, indexReader);
    }

    public int getConceptCount() {
        return conceptCount;
    }

    public void setConceptCount(int conceptCount) {
        this.conceptCount = conceptCount;
    }

    @Override
    public void close() {
        try {
            indexReader.close();
            termToConceptDirectory.close();
        } catch (IOException ex) {
            Logger.getLogger(Vectorizer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
