package be.vanoosten.esa;

import be.vanoosten.esa.tools.RelatedTokensFinder;
import be.vanoosten.esa.tools.Vectorizer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Philip van Oosten
 */
public abstract class WikiFactory implements AutoCloseable {
    public static String docType = "article";
    private Vectorizer vectorizer;
    private Analyzer analyzer;
    private RelatedTokensFinder relatedTokensFinder;
    private final Path indexRootPath;
    private final Path dumpFile;
    private final CharArraySet stopWords;
    private IndexSearcher relatedTermsSearcher;
    private FSDirectory relatedTermsIndex;
    private DirectoryReader relatedTermsIndexReader;

    protected WikiFactory(Path indexRootPath, Path dumpFile, CharArraySet stopWords) {
        this.indexRootPath = indexRootPath;
        this.dumpFile = dumpFile;
        this.stopWords = stopWords;
    }

    public final Path getIndexRootPath() {
        return indexRootPath;
    }

    public final Path getWikipediaDumpFile() {
        return dumpFile;
    }

    public final CharArraySet getStopWords() {
        return stopWords;
    }

    public final Analyzer getAnalyzer() {
        if (analyzer == null) {
            analyzer = new WikiAnalyzer(getStopWords());
        }
        return analyzer;
    }

    public synchronized final Vectorizer getOrCreateVectorizer() {
        if (vectorizer == null) {
            try {
                vectorizer = new Vectorizer(getIndexRootPath(), getAnalyzer());
            } catch (IOException ex) {
                Logger.getLogger(EnwikiFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return vectorizer;
    }

    @Override
    public synchronized final void close() throws Exception {
        if (vectorizer != null) {
            vectorizer.close();
            vectorizer = null;
        }
        if (relatedTermsSearcher != null) {
            relatedTermsIndex.close();
            relatedTermsIndexReader.close();
            relatedTermsSearcher = null;
        }
    }

    public synchronized final IndexSearcher getOrCreateRelatedTermsSearcher() {
        if (relatedTermsSearcher == null) {
            try {
                relatedTermsIndex = FSDirectory.open(getConceptTermIndexDirectory());
                relatedTermsIndexReader = DirectoryReader.open(relatedTermsIndex);
                QueryParser conceptQueryParser = new QueryParser(WikiIndexer.TEXT_FIELD, getAnalyzer());
                relatedTermsSearcher = new IndexSearcher(relatedTermsIndexReader);
            } catch (IOException ex) {
                Logger.getLogger(WikiFactory.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return relatedTermsSearcher;
    }

    public synchronized RelatedTokensFinder getOrCreateRelatedTokensFinder() {
        if (relatedTokensFinder == null) {
            IndexSearcher searcher = getOrCreateRelatedTermsSearcher();
            relatedTokensFinder = new RelatedTokensFinder(getOrCreateVectorizer(), relatedTermsIndexReader, relatedTermsSearcher);
        }
        return relatedTokensFinder;
    }

    public final Path getConceptTermIndexDirectory() {
        return Paths.get(getIndexRootPath().toString(), docType + "_conceptdoc");
    }
}
