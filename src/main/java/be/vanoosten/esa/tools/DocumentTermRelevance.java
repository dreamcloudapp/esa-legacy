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
        //Why does the dream not always exist?!
        getDocument();
        Query termQuery = new TermQuery(term);
        Query idQuery = new TermQuery(documentId);
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(termQuery, BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(idQuery, BooleanClause.Occur.FILTER));
        TopDocs topDocs = searcher.search(builder.build(), 1);
        if (topDocs.scoreDocs.length > 0) {
            return topDocs.scoreDocs[0].score;
        } else {
            //word isn't important
            return 0;
            //throw new IOException("Unable to find term '" + term.text() + "' in document '" + documentId.text() + "' when computing document term relevance.");
        }
    }

    private Document getDocument() throws IOException {
        if (document == null) {
            TermQuery query = new TermQuery(documentId);
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.scoreDocs.length > 0) {
                document = searcher.doc(topDocs.scoreDocs[0].doc);
            } else {
                System.out.println("Unable to find document '" + documentId.text() + "' when computing document term relevance.");
                //throw new IOException("Unable to find document '" + documentId.text() + "' when computing document term relevance.");
            }
        }
        return document;
    }
}
