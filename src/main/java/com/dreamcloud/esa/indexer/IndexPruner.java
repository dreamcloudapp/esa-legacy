package com.dreamcloud.esa.indexer;

import com.dreamcloud.esa.analyzer.AnalyzerOptions;
import com.dreamcloud.esa.analyzer.EsaAnalyzer;
import com.dreamcloud.esa.analyzer.TokenizerFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;

class LuceneToken {
    char[] term;
    float score;
}

class CachingTokenStream extends TokenStream {

    private int i = -1;
    private final ArrayList<LuceneToken> queue;

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);

    public CachingTokenStream() {
        this.queue = new ArrayList<>();
    }

    void produceToken(LuceneToken token) {
        queue.add(token);
    }

    public boolean incrementToken() throws IOException {
        i++;
        if (queue.size() <= i) {
            return false;
        }
        final LuceneToken token = queue.get(i);
        int tokenLength = token.term.length;
        termAtt.resizeBuffer(Math.max(termAtt.buffer().length, tokenLength));
        termAtt.setLength(tokenLength);
        final char[] buffer = termAtt.buffer();
        System.arraycopy(token.term, 0, buffer, 0, tokenLength);
        ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES);
        byteBuffer.putFloat(token.score);
        payloadAtt.setPayload(new BytesRef(byteBuffer.array()));
        return true;
    }
}

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
                    CachingTokenStream cachingTokenStream = new CachingTokenStream();
                    System.out.println("term: " + bytesRef.utf8ToString());
                    TopDocs td = SearchTerm(bytesRef, docSearcher);
                    Document doc = new Document();
                    doc.add(new StringField("text", bytesRef.utf8ToString(), Field.Store.NO));
                    for (ScoreDoc scoreDoc: td.scoreDocs) {
                        Document termDocDocument = termDocReader.document(scoreDoc.doc);
                        PostingsEnum postingsEnum;
                        LuceneToken token = new LuceneToken();
                        token.term = termDocDocument.get("title").toCharArray();
                        token.score = scoreDoc.score;
                        cachingTokenStream.produceToken(token);
                    }

                    FieldType type = new FieldType();
                    type.setStoreTermVectors(true);
                    type.setStoreTermVectorPositions(true);
                    type.setStoreTermVectorPayloads(true);
                    type.setStored(false);
                    type.setTokenized(true);
                    type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
                    doc.add(new Field("concept", cachingTokenStream, type));
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
