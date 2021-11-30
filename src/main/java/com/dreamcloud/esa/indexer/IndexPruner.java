package com.dreamcloud.esa.indexer;

import com.dreamcloud.esa.analyzer.AnalyzerOptions;
import com.dreamcloud.esa.analyzer.EsaAnalyzer;
import com.dreamcloud.esa.analyzer.TokenizerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Path;

public class IndexPruner {
    protected int windowSize;
    protected double maximumDrop;

    public IndexPruner(int windowSize, double maximumDrop) {
        this.windowSize = windowSize;
        this.maximumDrop = maximumDrop;
    }

    public IndexPruner() {
        this(100, 0.05);
    }

    public void prune(Path indexPath, Path prunedPath) throws IOException {
        final Directory termDocDirectory = FSDirectory.open(indexPath);
        final IndexReader termDocReader = DirectoryReader.open(termDocDirectory);
        AnalyzerOptions options = new AnalyzerOptions();
        options.tokenizerFactory = new TokenizerFactory() {
            public Tokenizer getTokenizer() {
                return new StandardTokenizer();
            }
        };
        final Analyzer analyzer = new EsaAnalyzer(options);
        final IndexSearcher docSearcher = new IndexSearcher(termDocReader);
        IndexWriterConfig conceptIndexWriterConfig = new IndexWriterConfig(analyzer);
        try (IndexWriter conceptIndexWriter = new IndexWriter(FSDirectory.open(prunedPath), conceptIndexWriterConfig)) {
            for(int l = 0; l < termDocReader.leaves().size(); l++) {
                System.out.println("leaf: " + l);
                TermsEnum terms = termDocReader.leaves().get(l).reader().terms("text").iterator();
                for (BytesRef bytesRef = terms.term(); terms.next() != null; ) {
                    System.out.println("term: " + bytesRef.utf8ToString());
                    TopDocs td = SearchTerm(bytesRef, docSearcher);
                    Document doc = new Document();
                    doc.add(new TextField("text", bytesRef.utf8ToString(), Field.Store.NO));
                    for (ScoreDoc scoreDoc: td.scoreDocs) {
                        Document termDocDocument = termDocReader.document(scoreDoc.doc);
                        String title = termDocDocument.get("title");
                        String id = termDocDocument.get(DreamIndexer.ID_FIELD);
                        doc.add(new StoredField("ids", id));
                        doc.add(new StoredField("names", title));
                        doc.add(new StoredField("weights", scoreDoc.score));
                    }
                    conceptIndexWriter.addDocument(doc);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static TopDocs SearchTerm(BytesRef bytesRef, IndexSearcher docSearcher) throws IOException {
        Term term = new Term("text", bytesRef);
        Query query = new TermQuery(term);
        int n = 1000;
        TopDocs td = docSearcher.search(query, n);
        if (n < td.totalHits.value) {
            n = (int) td.totalHits.value;
            td = docSearcher.search(query, n);
        }
        return td;
    }
}
