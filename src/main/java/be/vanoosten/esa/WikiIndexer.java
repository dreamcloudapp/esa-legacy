package be.vanoosten.esa;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Philip van Oosten
 */
public class WikiIndexer extends DefaultHandler implements AutoCloseable {
    private final SAXParserFactory saxFactory;
    private final ExecutorService executorService;
    private static int THREAD_COUNT = 1;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();
    private String wikiTitle;
    private AtomicInteger numIndexed = new AtomicInteger(0);
    private int numTotal = 0;

    public static final String TEXT_FIELD = "text";
    public static final String TITLE_FIELD = "title";
    Pattern pat;

    IndexWriter indexWriter;

    int minimumWordCount;
    int minimumIngoingLinks;
    int minimumOutgoingLinks;

    public int getMinimumWordCount() {
        return minimumWordCount;
    }

    public final void setMinimumWordCount(int minimumWordCount) {
        this.minimumWordCount = minimumWordCount;
    }

    public int getMinimumIngoingLinks() {
        return minimumIngoingLinks;
    }

    public void setMinimumIngoingLinks(int minimumIngoingLinks) {
        this.minimumIngoingLinks = minimumIngoingLinks;
    }

    public int getMinimumOutgoingLinks() {
        return minimumOutgoingLinks;
    }

    public void setMinimumOutgoingLinks(int minimumOutgoingLinks) {
        this.minimumOutgoingLinks = minimumOutgoingLinks;
    }

    public WikiIndexer(Analyzer analyzer, Directory directory) throws IOException {
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(true);
        saxFactory.setXIncludeAware(true);

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        String regex = "^[a-zA-z]+:.*";
        pat = Pattern.compile(regex);
        setMinimumWordCount(100);
        setMinimumIngoingLinks(3);
        setMinimumOutgoingLinks(3);
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    public void parseXmlDump(String path) {
        parseXmlDump(new File(path));
    }

    public void parseXmlDump(File file) {
        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            InputStream wikiInputStream = new FileInputStream(file);
            wikiInputStream = new BufferedInputStream(wikiInputStream);
            wikiInputStream = new BZip2CompressorInputStream(wikiInputStream, true);
            saxParser.parse(wikiInputStream, this);
            executorService.shutdown();
        } catch (ParserConfigurationException | SAXException | FileNotFoundException ex) {
            Logger.getLogger(WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("page".equals(localName)) {
            inPage = true;
        } else if (inPage && "title".equals(localName)) {
            inPageTitle = true;
            content = new StringBuilder();
        } else if (inPage && "text".equals(localName)) {
            inPageText = true;
            content = new StringBuilder();
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (inPage && inPageTitle && "title".equals(localName)) {
            inPageTitle = false;
            wikiTitle = content.toString();
        } else if (inPage && inPageText && "text".equals(localName)) {
            inPageText = false;
            String wikiTitleCopy = wikiTitle;
            String wikiText = content.toString();
            numTotal++;

            executorService.submit(() -> {
                try {
                    System.out.println("==========================================");
                    WikiAnalyzer analyzer = new WikiAnalyzer(Version.LUCENE_48, EnwikiFactory.getExtendedStopWords(), true);
                    TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, "[[" + wikiTitleCopy + "]] " + wikiText);
                    CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
                    TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
                    tokenStream.reset();
                    int linkCount = 0;
                    int tokenCount = 0;
                    String articleTitleToken = null;
                    while(tokenStream.incrementToken()) {
                        if (articleTitleToken == null) {
                            articleTitleToken = termAttribute.toString();
                            continue;
                        }

                        tokenCount++;
                        if ("il".equals(typeAttribute.type())) {
                            linkCount++;
                        }
                    }
                    tokenStream.close();

                    if (linkCount >= getMinimumOutgoingLinks() && tokenCount >= getMinimumWordCount() && index(wikiTitleCopy, wikiText)) {
                        int indexed = numIndexed.incrementAndGet();
                        if (indexed % 1000 == 0) {
                            System.out.println("" + indexed + "\t/ " + numTotal + "\t" + wikiTitleCopy);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else if (inPage && "page".equals(localName)) {
            inPage = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        content.append(ch, start, length);
    }

    boolean index(String title, String wikiText) throws IOException {
        Matcher matcher = pat.matcher(title);
        if (matcher.find() || title.startsWith("List of ") || title.contains("discography")) {
            return false;
        }
        Document doc = new Document();
        doc.add(new StoredField(TITLE_FIELD, title));
        doc.add(new TextField(TEXT_FIELD, wikiText, Field.Store.NO));
        indexWriter.addDocument(doc);
        return true;
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
    }

}
