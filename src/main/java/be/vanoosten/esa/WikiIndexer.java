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
    private static int BATCH_SIZE = 1000;
    Vector<WikipediaArticle> queue;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();
    private String wikiTitle;
    private int numTotal = 0;
    private int numLastTotal = 0;
    private int numAnalyzed = 0;
    private int numIndexed = 0;
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
    HashMap<Integer, String> indexTitles = new HashMap<>();
    HashMap <String, Integer> incomingLinkMap = new HashMap<>();

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
        setMinimumIngoingLinks(3);
        setMinimumOutgoingLinks(3);
        String regex = "^[a-zA-z]+:.*";
        pat = Pattern.compile(regex);
        queue = new Vector<>(BATCH_SIZE * THREAD_COUNT);
    }

    void reset() {
        articleIndex = 0;
        numLastTotal = 0;
        numTotal = 0;
        inPage = false;
        inPageTitle = false;
        inPageText = false;
        content = new StringBuilder();
        wikiTitle = null;
    }

    public void generateArticleInfo(File file) {
        reset();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        analyzer = new WikiAnalyzer(LUCENE_48, EnwikiFactory.getExtendedStopWords(), true);
        mode = "analyze";
        parseXmlDump(file);
        numAnalyzed = numTotal;

        System.out.println("----------------------------------------");
        System.out.println("Articles Analyzed:\t" + numAnalyzed);
        System.out.println("Articles Indexable:\t" + indexTitles.size());
        System.out.println("----------------------------------------");
    }

    public void indexArticles(File file) throws IOException {
        reset();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        analyzer = new WikiAnalyzer(LUCENE_48, EnwikiFactory.getExtendedStopWords());
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_48, analyzer);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        mode = "index";
        parseXmlDump(file);

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Analyzed:\t" + numAnalyzed);
        System.out.println("Articles Indexable:\t" + indexTitles.size());
        System.out.println("Articles Indexed:\t" + numIndexed);
        System.out.println("Articles Skipped:\t" + (numAnalyzed - numIndexed));
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Acceptance Rate:\t" + format.format(((double) numIndexed) / ((double) numAnalyzed)));
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
            numTotal++;
            inPageText = false;
            String wikiTitleCopy = wikiTitle;
            String wikiText = content.toString();

            Matcher matcher = pat.matcher(wikiTitle);
            if (matcher.find() || wikiTitle.startsWith("List of ") || wikiTitle.contains("discography")) {
                return;
            }

            queue.add(new WikipediaArticle(numTotal, wikiTitleCopy, wikiText));

            if (queue.size() == BATCH_SIZE * THREAD_COUNT) {
                if ("analyze".equals(mode)) {
                    this.processAnalysisQueue();
                } else {
                    this.processIndexQueue();
                }
                queue.clear();
                queue.ensureCapacity(BATCH_SIZE * THREAD_COUNT);
                if ("analyze".equals(mode)) {
                    System.out.println("Analyzed articles\t[" + numLastTotal + " - " + numTotal + "]");
                } else {
                    System.out.println("Indexed articles\t[" + numTotal + " / " + numAnalyzed + "]");
                }
                numLastTotal = numTotal;
            }
        } else if (inPage && "page".equals(localName)) {
            inPage = false;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        content.append(ch, start, length);
    }

    void processAnalysisQueue() {
        //Spawn up THREAD_COUNT threads and give each BATCH_SIZE articles
        ArrayList<Callable<Vector<WikipediaArticle>>> processors = new ArrayList<>();
        for (int i=0; i<THREAD_COUNT && (i * BATCH_SIZE) < queue.size(); i++) {
            final Vector<WikipediaArticle> articles = new Vector<>(BATCH_SIZE);
            for (int j = i * BATCH_SIZE; j<(((i+1) * BATCH_SIZE)) && j<queue.size(); j++) {
                articles.add(queue.get(j));
            }
            processors.add(() -> this.analyzeArticles(articles));
        }

        //Wait on all threads and then processes the results
        try{
            List<Future<Vector<WikipediaArticle>>> futures = executorService.invokeAll(processors);
            for(Future<Vector<WikipediaArticle>> future: futures){
                if (future.isDone()) {
                    Vector<WikipediaArticle> articles = future.get();
                    for (WikipediaArticle article: articles) {
                        //If the article is valid for indexing, map it's links
                        if (article.canIndex()) {
                            indexTitles.put(article.index, article.analysis.parsedTitle);
                            for (String link: article.getOutgoingLinks()) {
                                if (incomingLinkMap.containsKey(link)) {
                                    Integer count = incomingLinkMap.get(link);
                                    incomingLinkMap.put(link, count + 1);
                                } else {
                                    incomingLinkMap.put(link, 1);
                                }
                            }
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    Vector<WikipediaArticle> analyzeArticles(Vector<WikipediaArticle> articles) throws IOException {
        for (WikipediaArticle article: articles) {
            TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, "[[" + article.title + "]] " + article.text);
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
            tokenStream.reset();
            ArrayList<String> outgoingLinks = new ArrayList<>();
            int tokenCount = 0;
            String articleTitleToken = null;
            while(tokenStream.incrementToken()) {
                if (articleTitleToken == null) {
                    articleTitleToken = termAttribute.toString();
                    continue;
                }
                tokenCount++;
                if ("il".equals(typeAttribute.type())) {
                    outgoingLinks.add(termAttribute.toString());
                }
            }
            tokenStream.close();

            //Add article information
            if (articleTitleToken != null) {
                article.analysis = new WikipediaArticleAnalysis(articleTitleToken, outgoingLinks, tokenCount);
            }
        }
        return articles;
    }

    void processIndexQueue() {
        //Spawn up THREAD_COUNT threads and give each BATCH_SIZE articles
        ArrayList<Callable<Integer>> processors = new ArrayList<>();
        for (int i=0; i<THREAD_COUNT && (i * BATCH_SIZE) < queue.size(); i++) {
            Vector<WikipediaArticle> articles = new Vector<>(BATCH_SIZE);
            for (int j = i * BATCH_SIZE; j<(((i+1) * BATCH_SIZE)) && j<queue.size(); j++) {
                articles.add(queue.get(j));
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
    }

    Integer indexArticles (Vector<WikipediaArticle> articles) throws IOException {
        int indexed = 0;
        for (WikipediaArticle article: articles) {
            //Get title
            if (indexTitles.containsKey(article.index)) {
                String indexTitle = indexTitles.get(article.index);
                //Check incoming links
                if (incomingLinkMap.containsKey(indexTitle) && incomingLinkMap.get(indexTitle) > 1) {
                    index(article.title, article.text);
                    indexed++;
                }
            }
        }
        return indexed;
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
