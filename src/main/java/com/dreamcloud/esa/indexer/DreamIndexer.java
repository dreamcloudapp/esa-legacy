package com.dreamcloud.esa.indexer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Philip van Oosten
 */
public class DreamIndexer extends DefaultHandler implements AutoCloseable, Indexer {
    public static final String TEXT_FIELD = "text";
    public static final String TITLE_FIELD = "title";
    public static final String ID_FIELD = "id";
    public static final String USER_FIELD = "user";

    private final SAXParserFactory saxFactory;
    private StringBuilder content = new StringBuilder();
    private String dreamId = "";
    private String dreamTitle = "";
    private String dreamContent = "";
    private String dreamUserId = "";
    private int numTotal = 0;
    private int numIndexed = 0;

    IndexWriter indexWriter;
    IndexerOptions options;

    public DreamIndexer(IndexerOptions options) {
        this.options = options;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void index(File file) throws IOException {
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(options.analyzerFactory.getAnalyzer());
        indexWriter = new IndexWriter(options.indexDirectory, indexWriterConfig);

        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            InputStream dreamInputStream = new FileInputStream(file);
            InputStream bufferedInputStream = new BufferedInputStream(dreamInputStream);
            saxParser.parse(bufferedInputStream, this);
            bufferedInputStream.close();
            dreamInputStream.close();

        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(DreamIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }

        indexWriter.commit();

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Dreams Count:\t" + numTotal);
        System.out.println("Dreams Indexed:\t" + numIndexed);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Acceptance Rate:\t" + format.format(((double) numIndexed) / ((double) numTotal)));
        System.out.println("----------------------------------------");
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        switch (localName) {
            case "id":
                dreamId = content.toString().trim();
                break;
            case "title":
                dreamTitle = content.toString().trim();
                break;
            case "content":
                dreamContent = content.toString().trim();
                break;
            case "user":
                dreamUserId = content.toString().trim();
                break;
            case "dream":
                numTotal++;
                String dreamText = (dreamTitle + " " + dreamContent).trim();
                if (dreamText.length() > 0) {
                    try {
                        index(dreamId, dreamTitle, dreamText, dreamUserId);
                        numIndexed++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        dreamId = dreamContent = dreamTitle = dreamUserId = "";
                    }
                }
        }
        content = new StringBuilder();
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }

    void index(String id, String title, String text, String userId) throws Exception {
        if (options.preprocessor != null) {
            text = options.preprocessor.process(text);
        }

        Document doc = new Document();
        doc.add(new StringField(ID_FIELD, id, Field.Store.YES));
        doc.add(new StringField(TITLE_FIELD, title, Field.Store.YES));
        doc.add(new TextField(TEXT_FIELD, text, Field.Store.NO));
        doc.add(new StringField(USER_FIELD, userId, Field.Store.YES));
        indexWriter.addDocument(doc);
    }

    public void close() throws IOException {
        indexWriter.close();
    }
}

