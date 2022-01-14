package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.fs.FileSystem;
import com.dreamcloud.esa.fs.TermIndexWriter;
import com.dreamcloud.esa.fs.TermScore;
import com.dreamcloud.esa.fs.TermScoreWriter;
import com.dreamcloud.esa.vectorizer.PruneOptions;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
            termScores.put(term, byteVector);
        }
    }

    public void close() throws IOException {
        if (collectionInfo == null) {
            throw new RuntimeException("You must call writeCollectionInfo prior to close().");
        }

        TermIndexWriter termIndexWriter = new TermIndexWriter(collectionInfo.numDocs, collectionInfo.getAverageDocumentLength());
        termIndexWriter.open(termIndexFile);

        TermScoreWriter termScoreWriter = new TermScoreWriter();
        termScoreWriter.open(documentScoreFile);

        //Write the term index
        for (String term: termScores.keySet()) {
            TfIdfByteVector byteVector = termScores.get(term);
            int numScores = byteVector.getSize() / FileSystem.DOCUMENT_SCORE_BYTES;
            termIndexWriter.writeTerm(term, numScores);

            //Need to sort the scores and potentially prune
            TermScore[] scores = new TermScore[numScores];
            ByteBuffer scoreBuffer = byteVector.getByteBuffer();
            for (int scoreIdx = 0; scoreIdx < numScores; scoreIdx++) {
                TermScore termScore = new TermScore();
                termScore.document = scoreBuffer.getInt();
                termScore.score = scoreBuffer.getFloat();
                scores[scoreIdx] = termScore;
            }
            Arrays.sort(scores, (s1, s2) -> Float.compare(s2.score, s1.score));
            termScoreWriter.writeTermScores(scores);
            //termScoreWriter.writeTermScores(byteVector.getBytes());
        }
        termIndexWriter.close();
        termScoreWriter.close();
    }
}
