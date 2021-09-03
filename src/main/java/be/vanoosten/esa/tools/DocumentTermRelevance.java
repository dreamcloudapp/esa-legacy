package be.vanoosten.esa.tools;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import java.io.IOException;

public class DocumentTermRelevance {
    IndexSearcher searcher;
    Term documentId;
    Document document;

    public DocumentTermRelevance(Term documentId, IndexSearcher searcher) {
        this.searcher = searcher;
        this.documentId = documentId;
    }

    public float score(Term term) throws IOException {
        Document document = this.getDocument();
        Query termQuery = new TermQuery(term);
        Query idQuery = new TermQuery(documentId);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(termQuery, BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(idQuery, BooleanClause.Occur.FILTER));
        TopDocs topDocs = searcher.search(builder.build(), 1);
        if (topDocs.scoreDocs.length > 0) {
            return topDocs.scoreDocs[0].score;
        } else {
            throw new IOException("Unable to find term in document document when computing document term relevance.");
        }
    }

    private Document getDocument() throws IOException {
        if (document == null) {
            TermQuery query = new TermQuery(documentId);
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.scoreDocs.length > 0) {
                document = searcher.doc(topDocs.scoreDocs[0].doc);
            } else {
                throw new IOException("Unable to find document when computing document term relevance.");
            }
        }
        return document;
    }
}
