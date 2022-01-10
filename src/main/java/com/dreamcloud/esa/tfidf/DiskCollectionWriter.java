package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.fs.FileSystem;
import com.dreamcloud.esa.fs.TermIndexWriter;
import com.dreamcloud.esa.fs.TermScoreWriter;
import com.dreamcloud.esa.vectorizer.PruneOptions;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DiskCollectionWriter implements CollectionWriter {
    private final File termIndexFile;
    private final File documentScoreFile;
    protected Map<String, TfIdfByteVector> termScores = new HashMap<>();
    protected CollectionInfo collectionInfo;
    protected PruneOptions pruneOptions;

    public DiskCollectionWriter(File termIndexFile, File documentScoreFile, PruneOptions pruneOptions) {
        this.termIndexFile = termIndexFile;
        this.documentScoreFile = documentScoreFile;
        this.pruneOptions = pruneOptions;
    }

    public DiskCollectionWriter(File termIndexFile, File documentScoreFile) {
        this(termIndexFile, documentScoreFile, null);
    }

    public void writeCollectionInfo(CollectionInfo collectionInfo) {
        //Just save this, don't write anything till close().
        this.collectionInfo = collectionInfo;
    }

    public void writeDocumentScores(int documentId, TfIdfScore[] scores) {
        for (TfIdfScore tfIdfScore: scores) {
            String term = tfIdfScore.getTerm();
            float score = (float) tfIdfScore.getScore();
            TfIdfByteVector byteVector = termScores.getOrDefault(term, new TfIdfByteVector(256 * FileSystem.DOCUMENT_SCORE_BYTES));
            ByteBuffer byteBuffer = ByteBuffer.allocate(FileSystem.DOCUMENT_SCORE_BYTES);
            byteBuffer.putInt(documentId);
            byteBuffer.putFloat(score);
            byteVector.addBytes(byteBuffer.array());
        }
    }

    public void close() throws IOException {
        if (collectionInfo == null) {
            throw new RuntimeException("You must call writeCollectionInfo prior to close().");
        }

        TermIndexWriter termIndexWriter = new TermIndexWriter(collectionInfo.numDocs);
        termIndexWriter.open(termIndexFile);

        TermScoreWriter termScoreWriter = new TermScoreWriter();
        termScoreWriter.open(documentScoreFile);

        //Write the term index
        for (String term: termScores.keySet()) {
            TfIdfByteVector byteVector = termScores.get(term);
            termIndexWriter.writeTerm(term, byteVector.getSize() / FileSystem.DOCUMENT_SCORE_BYTES);

            //Need to sort the scores and potentially prune

            termScoreWriter.writeTermScores(byteVector.getBytes());
        }
        termIndexWriter.close();
        termScoreWriter.close();
    }
}
