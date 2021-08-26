package be.vanoosten.esa;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
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
public class WiktionaryIndexer<HashTable> extends DefaultHandler implements AutoCloseable {
    private final SAXParserFactory saxFactory;
    private ExecutorService executorService;
    private static int THREAD_COUNT = 16;
    private static int BATCH_SIZE = 100;
    private static int MAX_EXPECTED_ARTICLES = 10000000;
    WikipediaArticle[] fixedQueue = new WikipediaArticle[THREAD_COUNT * BATCH_SIZE];
    int queueSize = 0;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();
    private String wikiTitle;
    private int numTotal = 0;
    private int numLastTotal = 0;
    private int numIndexed = 0;
    private int numSkipped = 0;

    private Directory directory;
    private Analyzer analyzer;

    public static final String TEXT_FIELD = "text";
    public static final String TITLE_FIELD = "title";
    Pattern pat;
    IndexWriter indexWriter;

    public WiktionaryIndexer(Directory directory) {
        this.directory = directory;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
        String regex = "^[a-zA-z]+:.*";
        pat = Pattern.compile(regex);
    }

    void reset() {
        numLastTotal = 0;
        numTotal = 0;
        inPage = false;
        inPageTitle = false;
        inPageText = false;
        content = new StringBuilder();
        wikiTitle = null;
    }

    public void indexArticles(File file) throws IOException {
        reset();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        analyzer = WikiAnalyzerFactory.getAnalyzer();
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_48, new StandardAnalyzer(LUCENE_48));
        indexWriterConfig.setSimilarity(SimilarityFactory.getSimilarity());
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        parseXmlDump(file);
        //There may be queue items left over
        if (queueSize > 0) {
            this.processQueue();
        }

        analyzer.close();
        executorService.shutdown();
        try {
            executorService.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Indexed:\t" + numIndexed);
        System.out.println("Articles Skipped:\t" + numSkipped);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Acceptance Rate:\t" + format.format(((double) numIndexed) / ((double) numSkipped)));
        System.out.println("----------------------------------------");
    }

    public void parseXmlDump(File file) {
        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            InputStream wikiInputStream = new FileInputStream(file);
            InputStream bufferedInputStream = new BufferedInputStream(wikiInputStream);
            InputStream bzipInputStream = new BZip2CompressorInputStream(bufferedInputStream, true);
            saxParser.parse(bzipInputStream, this);
            bzipInputStream.close();
            bufferedInputStream.close();
            wikiInputStream.close();

        } catch (ParserConfigurationException | SAXException | FileNotFoundException ex) {
            Logger.getLogger(WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
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
            numTotal++;
            inPageText = false;
            String wikiTitleCopy = wikiTitle;
            String wikiText = content.toString();

            /*Matcher matcher = pat.matcher(wikiTitle);
            if (matcher.find() || wikiTitle.startsWith("List of ") || wikiTitle.contains("discography")) {
                return;
            }*/

            fixedQueue[queueSize++] = new WikipediaArticle(numTotal, wikiTitleCopy, wikiText);
            if (queueSize == BATCH_SIZE * THREAD_COUNT) {
                this.processQueue();
                queueSize = 0;
            }
        } else if (inPage && "page".equals(localName)) {
            inPage = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }

    void processQueue() {
        //Spawn up THREAD_COUNT threads and give each BATCH_SIZE articles
        ArrayList<Callable<Integer>> processors = new ArrayList<>();
        for (int i=0; i<THREAD_COUNT && (i * BATCH_SIZE) < queueSize; i++) {
            Vector<WikipediaArticle> articles = new Vector<>(BATCH_SIZE);
            for (int j = i * BATCH_SIZE; j<(((i+1) * BATCH_SIZE)) && j<queueSize; j++) {
                articles.add(fixedQueue[j]);
            }
            processors.add(() -> this.indexArticles(articles));
        }

        //Wait on all threads and then processes the results
        try{
            List<Future<Integer>> futures = executorService.invokeAll(processors);
            for(Future<Integer> future: futures){
                if (future.isDone()) {
                    Integer indexed = future.get();
                    numIndexed += indexed;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.exit(1);
        }

        for (int i=0; i<THREAD_COUNT * BATCH_SIZE; i++) {
            fixedQueue[i] = null;
        }
        queueSize = 0;
        System.out.println("Indexed articles\t[" + numLastTotal + " - " + numTotal + "]");
        numLastTotal = numTotal;
    }

    Integer indexArticles (Vector<WikipediaArticle> articles) throws IOException {
        int indexed = 0;
        for (WikipediaArticle article: articles) {
            if(index(article.title, article.text)) {
                indexed++;
            }
        }
        return indexed;
    }

    boolean index(String title, String wikiText) throws IOException {
        Document doc = new Document();
        doc.add(new StoredField(TITLE_FIELD, title));

        TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, wikiText);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
        tokenStream.reset();
        boolean hasEnglish = false;
        while(tokenStream.incrementToken()) {
            if ("h".equals(typeAttribute.type()) && "english".equals(termAttribute.toString())) {
                System.out.println(wikiText);
                System.exit(1);
            }
        }
        tokenStream.close();

        //doc.add(new TextField(TEXT_FIELD, wikiText, Field.Store.NO));
        //indexWriter.addDocument(doc);
        return true;
    }

    @Override
    public void close() throws IOException {
        indexWriter.close();
    }

}
