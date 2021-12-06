package com.dreamcloud.esa.indexer;

import com.dreamcloud.esa.analyzer.WikipediaArticle;
import com.dreamcloud.esa.database.InverseTermMap;
import com.dreamcloud.esa.database.MySQLConnection;
import com.dreamcloud.esa.database.TermScore;
import com.dreamcloud.esa.database.TermScores;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexPruner {
    protected int windowSize;
    protected double maximumDrop;
    protected AtomicInteger processedTerms = new AtomicInteger(0);
    protected int totalTerms = 0;

    public IndexPruner(int windowSize, double maximumDrop) {
        this.windowSize = windowSize;
        this.maximumDrop = maximumDrop;
    }

    public IndexPruner() {
        this(100, 0.05);
    }

    public void prune(Path indexPath, Path prunedPath) throws IOException, SQLException {
        Directory termDocDirectory = FSDirectory.open(indexPath);
        IndexReader termDocReader = DirectoryReader.open(termDocDirectory);
        IndexSearcher docSearcher = new IndexSearcher(termDocReader);
        ExecutorService executorService = Executors.newFixedThreadPool(termDocReader.leaves().size());
        ArrayList<Callable<Integer>> processors = new ArrayList<>();
        for (int i=0; i<termDocReader.leaves().size(); i++) {
            TermsEnum terms = termDocReader.leaves().get(i).reader().terms("text").iterator();
            processors.add(() -> this.pruneTerms(terms, docSearcher));
        }

        try{
            List<Future<Integer>> futures = executorService.invokeAll(processors);
            for(Future<Integer> future: futures){
                if (future.isDone()) {
                    System.out.println("Thread completed.");
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }


        for(int l = 0; l < termDocReader.leaves().size(); l++) {
            TermsEnum terms = termDocReader.leaves().get(l).reader().terms("text").iterator();
            for (BytesRef bytesRef = terms.term(); terms.next() != null; ) {
                if (bytesRef.length > 32) {
                    //todo: move to length limit filter
                    continue;
                }
                totalTerms++;
            }
        }
    }

    private Integer pruneTerms(TermsEnum terms, IndexSearcher docSearcher) throws IOException, SQLException {
        InverseTermMap termMap = new InverseTermMap();
        for (BytesRef bytesRef = terms.term(); terms.next() != null; ) {
            if (bytesRef.length > 32) {
                //todo: move to length limit filter
                continue;
            }
            TermScores scores = new TermScores(terms.term());
            TopDocs td = SearchTerm(scores.term, docSearcher);
            for (ScoreDoc scoreDoc: td.scoreDocs) {
                scores.scores.add(new TermScore(scoreDoc.doc, scoreDoc.score));
            }
            termMap.saveTermScores(scores);

            int termCount = processedTerms.getAndIncrement();
            if (termCount % 100 == 0) {
                System.out.println("processed term" + "\t[" + termCount + " / " + totalTerms + "] (" + bytesRef.utf8ToString() + ")");
            }
        }
        return 0;
    }

    private static TopDocs SearchTerm(BytesRef bytesRef, IndexSearcher docSearcher) throws IOException {
        Term term = new Term("text", bytesRef);
        Query query = new TermQuery(term);
        int n = 100; //docSearcher.getIndexReader().maxDoc();
        TopDocs td = docSearcher.search(query, n);
        if (n < td.totalHits.value) {
            n = (int) td.totalHits.value;
            td = docSearcher.search(query, n);
        }
        return td;
    }
}
