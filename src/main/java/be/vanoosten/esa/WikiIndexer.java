package be.vanoosten.esa;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
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

import static org.apache.lucene.util.Version.LUCENE_48;

/**
 *
 * @author Philip van Oosten
 */
public class WikiIndexer<HashTable> extends DefaultHandler implements AutoCloseable {
    private final SAXParserFactory saxFactory;
    private ExecutorService executorService;
    private static int THREAD_COUNT = 16;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();
    private String wikiTitle;
    private int numTotal = 0;
    private AtomicInteger numAnalyzed = new AtomicInteger(0);
    private AtomicInteger numIndexed = new AtomicInteger(0);
    private AtomicInteger numSkipped = new AtomicInteger(0);
    private AtomicInteger numSkippedTitle = new AtomicInteger(0);
    private AtomicInteger numSkippedWords = new AtomicInteger(0);
    private AtomicInteger numSkippedIncoming = new AtomicInteger(0);
    private AtomicInteger numSkippedOutgoing = new AtomicInteger(0);
    private int articleIndex = 0;

    private Directory directory;
    private Analyzer analyzer;
    private String mode;

    public static final String TEXT_FIELD = "text";
    public static final String TITLE_FIELD = "title";
    Pattern pat;
    IndexWriter indexWriter;

    //Requirements for articles we'll index
    int minimumWordCount; //i.e. tokens including stopwords
    int minimumIngoingLinks;
    int minimumOutgoingLinks;
    Vector<String> incomingLinks = new Vector<>(4096);
    Set<Integer> acceptedArticles = Collections.newSetFromMap(new ConcurrentHashMap<>());

    ConcurrentHashMap <String, Integer> incomingLinkMap = new ConcurrentHashMap<>();

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

    public WikiIndexer(Directory directory) {
        this.directory = directory;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(true);
        saxFactory.setXIncludeAware(true);
        setMinimumWordCount(10);
        setMinimumIngoingLinks(2);
        setMinimumOutgoingLinks(2);
        String regex = "^[a-zA-z]+:.*";
        pat = Pattern.compile(regex);
    }

    void reset() {
        articleIndex = 0;
        numTotal = 0;
        inPage = false;
        inPageTitle = false;
        inPageText = false;
        content = new StringBuilder();
        wikiTitle = null;
    }

    public void generateArticleInfo(File file) {
        reset();
        incomingLinks = new Vector<>();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        analyzer = new WikiAnalyzer(LUCENE_48, EnwikiFactory.getExtendedStopWords(), true);
        mode = "analyze";
        parseXmlDump(file);
    }

    public void indexArticles(File file) throws IOException {
        reset();

        //Build the incoming link map from the vector
        for (String link : incomingLinks) {
            if (incomingLinkMap.containsKey(link)) {
                Integer count = incomingLinkMap.get(link);
                incomingLinkMap.put(link, ++count);
            } else {
                incomingLinkMap.put(link, 1);
            }
        }
        //Clean up vector
        incomingLinks.clear();

        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        analyzer = new WikiAnalyzer(LUCENE_48, EnwikiFactory.getExtendedStopWords());
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        mode = "index";
        parseXmlDump(file);

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Analyzed:\t" + numAnalyzed.get());
        System.out.println("Articles Indexed:\t" + numIndexed.get());
        System.out.println("Articles Skipped:\t" + numSkipped.get());
        System.out.println("    Reason=title:\t" + numSkippedTitle.get());
        System.out.println("    Reason=words:\t" + numSkippedWords.get());
        System.out.println("    Reason=i-links:\t" + numSkippedIncoming.get());
        System.out.println("    Reason=o-links:\t" + numSkippedOutgoing.get());
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Acceptance Rate:\t" + format.format(((double) numIndexed.get()) / ((double) numTotal)));
        System.out.println("----------------------------------------");
    }

    public void parseXmlDump(File file) {
        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            InputStream wikiInputStream = new FileInputStream(file);
            wikiInputStream = new BufferedInputStream(wikiInputStream);
            wikiInputStream = new BZip2CompressorInputStream(wikiInputStream, true);
            saxParser.parse(wikiInputStream, this);
            executorService.shutdown();
            Boolean result = executorService.awaitTermination(4, TimeUnit.HOURS);
            wikiInputStream.close();
            if (result) {
                System.out.println("All threads successfully completed.");
            } else {
                System.out.println("Thread timeout exceeded.");
            }
        } catch (ParserConfigurationException | SAXException | FileNotFoundException ex) {
            Logger.getLogger(WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
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
    public void endElement(String uri, String localName, String qName) {
        if (inPage && inPageTitle && "title".equals(localName)) {
            inPageTitle = false;
            wikiTitle = content.toString();
        } else if (inPage && inPageText && "text".equals(localName)) {
            inPageText = false;
            String wikiTitleCopy = wikiTitle;
            String wikiText = content.toString();
            int articleIndexCopy = articleIndex;
            numTotal++;

            Matcher matcher = pat.matcher(wikiTitle);
            if (matcher.find() || wikiTitle.startsWith("List of ") || wikiTitle.contains("discography")) {
                //Don't bother with the threads, just skip straight away
                if("analyze".equals(mode)) {
                    numAnalyzed.incrementAndGet();
                    numSkipped.incrementAndGet();
                    numSkippedTitle.incrementAndGet();
                }
                return;
            }

            executorService.submit(() -> {
                try {
                    if ("analyze".equals(mode)) {
                        TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, "[[" + wikiTitleCopy + "]] " + wikiText);
                        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
                        TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
                        tokenStream.reset();
                        int outgoingLinkCount = 0;
                        int tokenCount = 0;
                        String articleTitleToken = null;
                        while(tokenStream.incrementToken()) {
                            if (articleTitleToken == null) {
                                articleTitleToken = termAttribute.toString();
                                continue;
                            }

                            tokenCount++;
                            if ("il".equals(typeAttribute.type())) {
                                outgoingLinkCount++;
                                if (articleTitleToken != null) {
                                    incomingLinks.add(termAttribute.toString());
                                }
                            }
                        }
                        tokenStream.close();
                        Integer analyzed = numAnalyzed.incrementAndGet();

                        if (analyzed % 1000 == 0) {
                            System.out.println("Analyzed " + analyzed + "\t/ " + numTotal + "\t" + wikiTitleCopy);
                        }

                        //Add article information
                        if (articleTitleToken != null) {
                            if (tokenCount >= getMinimumWordCount() && outgoingLinkCount >= getMinimumOutgoingLinks()) {
                                acceptedArticles.add(articleIndexCopy);
                            } else {
                                if (tokenCount < getMinimumWordCount()) {
                                    numSkippedWords.incrementAndGet();
                                }
                                if (outgoingLinkCount < getMinimumOutgoingLinks()) {
                                    numSkippedOutgoing.incrementAndGet();
                                }
                                numSkipped.incrementAndGet();
                            }
                        }
                    } else {
                        //Get article information
                        if (acceptedArticles.contains(articleIndexCopy)) {
                            acceptedArticles.remove(articleIndexCopy);

                            if (incomingLinkMap.containsKey(wikiTitleCopy) && incomingLinkMap.get(wikiTitleCopy) >= getMinimumIngoingLinks()) {
                                index(wikiTitleCopy, wikiText);
                                int indexed = numIndexed.incrementAndGet();
                                if (indexed % 1000 == 0) {
                                    System.out.println("Indexed " + indexed + "\t/ " + numTotal + "\t" + wikiTitleCopy);
                                }
                            } else {
                                numSkipped.incrementAndGet();
                                numSkippedIncoming.incrementAndGet();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            articleIndex++;
        } else if (inPage && "page".equals(localName)) {
            inPage = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        content.append(ch, start, length);
    }

    void index(String title, String wikiText) throws IOException {
        Document doc = new Document();
        doc.add(new StoredField(TITLE_FIELD, title));
        doc.add(new TextField(TEXT_FIELD, wikiText, Field.Store.NO));
        indexWriter.addDocument(doc);
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
    }

}
