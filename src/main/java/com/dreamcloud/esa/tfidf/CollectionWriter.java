package com.dreamcloud.esa.tfidf;

import java.io.IOException;

public interface CollectionWriter {
    public void writeCollectionInfo(CollectionInfo collectionInfo);
    public void writeDocumentScores(int documentId, TfIdfScore[] scores);
    public void close() throws IOException;
}
