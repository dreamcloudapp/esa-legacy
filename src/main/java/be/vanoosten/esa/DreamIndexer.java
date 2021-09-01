package be.vanoosten.esa;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Philip van Oosten
 */
public class DreamIndexer extends DefaultHandler implements AutoCloseable, Indexer {
    private final SAXParserFactory saxFactory;
    private StringBuilder content = new StringBuilder();
    private String dreamId = "";
    private String dreamTitle = "";
    private String dreamContent = "";
    private String dreamUserId = "";
    private int numTotal = 0;
    private int numIndexed = 0;

    private Directory directory;
    private Analyzer analyzer;
    IndexWriter indexWriter;

    public static final String TEXT_FIELD = "text";
    public static final String TITLE_FIELD = "title";
    public static final String ID_FIELD = "id";
    public static final String USER_FIELD = "user";

    public DreamIndexer(Directory directory) {
        this.directory = directory;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void analyze(File file) {
        //nothing to do here
    }

    public void index(File file) throws IOException {
        Map<String, Analyzer> analyzerPerField = new HashMap<>();
        analyzerPerField.put(TEXT_FIELD, AnalyzerFactory.getDreamAnalyzer());
        analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(Version.LUCENE_48), analyzerPerField);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        indexWriterConfig.setSimilarity(SimilarityFactory.getSimilarity());
        indexWriter = new IndexWriter(directory, indexWriterConfig);

        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            InputStream dreamInputStream = new FileInputStream(file);
            InputStream bufferedInputStream = new BufferedInputStream(dreamInputStream);
            saxParser.parse(bufferedInputStream, this);
            bufferedInputStream.close();
            dreamInputStream.close();

        } catch (ParserConfigurationException | SAXException | FileNotFoundException ex) {
            Logger.getLogger(DreamIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DreamIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }

        indexWriter.commit();
        analyzer.close();

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
                    } catch (IOException e) {
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

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {

    }

    void index(String id, String title, String text, String userId) throws IOException {
        Document doc = new Document();
        doc.add(new TextField(ID_FIELD, id, Field.Store.YES));
        doc.add(new StoredField(TITLE_FIELD, title));
        doc.add(new TextField(TEXT_FIELD, text, Field.Store.NO));
        doc.add(new TextField(USER_FIELD, userId, Field.Store.YES));
        indexWriter.addDocument(doc);
    }

    public void close() throws IOException {
        indexWriter.close();
    }
}

