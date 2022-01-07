package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.fs.FileSystem;
import com.dreamcloud.esa.fs.TermIndexWriter;
import com.dreamcloud.esa.fs.TermScoreWriter;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class DiskCollectionWriter implements CollectionWriter {
    private final File termIndexFile;
    private final File documentScoreFile;

    public DiskCollectionWriter(File termIndexFile, File documentScoreFile) {
        this.termIndexFile = termIndexFile;
        this.documentScoreFile = documentScoreFile;
    }

    protected Map<String, byte[]> termScores = new HashMap<>();
    protected CollectionInfo collectionInfo;

    public void writeCollectionInfo(CollectionInfo collectionInfo) {
        //Just save this, don't write anything till close().
        this.collectionInfo = collectionInfo;
    }

    public void writeDocumentScores(int documentId, TfIdfScore[] scores) {
        for (TfIdfScore tfIdfScore: scores) {
            String term = tfIdfScore.getTerm();
            float score = (float) tfIdfScore.getScore();

            byte[] termScore = termScores.getOrDefault(term, new byte[0]);
            ByteBuffer byteBuffer = ByteBuffer.allocate(termScore.length + FileSystem.DOCUMENT_SCORE_BYTES);
            byteBuffer.put(termScore);
            byteBuffer.putInt(documentId);
            byteBuffer.putFloat(score);
            termScores.put(term, byteBuffer.array());
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
            byte[] scores = termScores.get(term);
            termIndexWriter.writeTerm(term, scores.length / FileSystem.DOCUMENT_SCORE_BYTES);
            termScoreWriter.writeTermScores(scores);
        }
        termIndexWriter.close();
        termScoreWriter.close();
    }
}
