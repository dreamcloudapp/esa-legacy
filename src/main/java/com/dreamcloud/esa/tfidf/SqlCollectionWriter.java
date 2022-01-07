package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.database.TfIdfScoreRepository;

public class SqlCollectionWriter implements CollectionWriter {
    private final TfIdfScoreRepository scoreRepository;

    public SqlCollectionWriter(TfIdfScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    public void writeCollectionInfo(CollectionInfo collectionInfo) {
        scoreRepository.saveDocumentCount(collectionInfo.numDocs);
        scoreRepository.saveTermDocumentFrequencies(collectionInfo.getDocumentFrequencies());
    }

    public void writeDocumentScores(int documentId, TfIdfScore[] scores) {
        scoreRepository.saveTfIdfScores(documentId, scores);
    }

    public void close() {

    }
}
