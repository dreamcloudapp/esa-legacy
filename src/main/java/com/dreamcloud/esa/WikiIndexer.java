package com.dreamcloud.esa;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.dreamcloud.esa.analyzer.AnalyzerFactory;
import com.dreamcloud.esa.analyzer.WikipediaArticle;
import com.dreamcloud.esa.analyzer.WikipediaArticleAnalysis;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
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
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Philip van Oosten
 */
public class WikiIndexer extends DefaultHandler implements AutoCloseable, Indexer {
    private final SAXParserFactory saxFactory;
    private ExecutorService executorService;
    private static int THREAD_COUNT = 16;
    private static int BATCH_SIZE = 100;
    private static int MAX_EXPECTED_ARTICLES = 32000000;
    WikipediaArticle[] fixedQueue = new WikipediaArticle[THREAD_COUNT * BATCH_SIZE];
    int queueSize = 0;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();
    private String wikiTitle;
    private int numTotal = 0;
    private int numLastTotal = 0;
    private int numAnalyzed = 0;
    private int numIndexed = 0;
    private int numIndexable = 0;

    private Directory directory;
    private Analyzer analyzer;
    private String mode;

    public static final String TEXT_FIELD = "text";
    public static final String TITLE_FIELD = "title";
    Pattern pat;
    IndexWriter indexWriter;

    String[] indexTitles;
    MutableObjectIntMap<String> incomingLinkMap = ObjectIntMaps.mutable.empty();
    static StanfordCoreNLP stanfordPipeline;
    static Analyzer lemmaAnalyzer;
    static boolean useStanfordLemmas = true;

    //takes a while
    static StanfordCoreNLP getStanfordPipeline() {
        if (stanfordPipeline == null) {
            Properties props = new Properties();
            // set the list of annotators to run
            props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
            // build pipeline
            stanfordPipeline = new StanfordCoreNLP(props);
        }
        return stanfordPipeline;
    }

    static Analyzer getLemmaAnalyzer() {
        if (lemmaAnalyzer == null) {
            lemmaAnalyzer = AnalyzerFactory.getLemmaAnalyzer();
        }
        return lemmaAnalyzer;
    }

    String getStanfordLemmatizedArticle(String articleText) throws IOException {
        // get valid tokens for article
        Analyzer analyzer = getLemmaAnalyzer();
        StringBuilder analyzedText = new StringBuilder();
        TokenStream tokenStream = analyzer.tokenStream(TEXT_FIELD, articleText);
        CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        while(tokenStream.incrementToken()) {
            analyzedText.append(termAttribute.toString()).append(" ");
        }
        tokenStream.close();

        //get stanford lemmas
        StanfordCoreNLP pipeline = getStanfordPipeline();
        CoreDocument document = pipeline.processToCoreDocument(analyzedText.toString());
        StringBuilder lemmatizedText = new StringBuilder();
        for (CoreLabel token: document.tokens()) {
            lemmatizedText.append(token.lemma()).append(" ");
        }
        return lemmatizedText.toString();
    }

    public WikiIndexer(Directory directory) {
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

    public void analyze(File file) {
        reset();
        indexTitles = new String[MAX_EXPECTED_ARTICLES];
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        analyzer = AnalyzerFactory.getLinkAnalyzer();
        mode = "analyze";
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

        System.gc();

        numAnalyzed = numTotal;

        System.out.println("----------------------------------------");
        System.out.println("Articles Analyzed:\t" + numAnalyzed);
        System.out.println("Articles Indexable:\t" + numIndexable);
        System.out.println("----------------------------------------");
    }

    public void index(File file) throws IOException {
        reset();
        executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        if (useStanfordLemmas) {
            analyzer = AnalyzerFactory.getPostLemmaAnalyzer();
        } else {
            analyzer = AnalyzerFactory.getAnalyzer();
        }

        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriter = new IndexWriter(directory, indexWriterConfig);
        mode = "index";
        parseXmlDump(file);
        //There may be queue items left over
        if (queueSize > 0) {
            this.processQueue();
        }

        indexWriter.commit();
        analyzer.close();
        executorService.shutdown();
        try {
            executorService.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Analyzed:\t" + numAnalyzed);
        System.out.println("Articles Indexable:\t" + numIndexable);
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

            Matcher matcher = pat.matcher(wikiTitle);
            if (matcher.find() || wikiTitle.startsWith("List of ") || wikiTitle.contains("discography")) {
                return;
            }

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


    void processAnalysisQueue() {
        //Spawn up THREAD_COUNT threads and give each BATCH_SIZE articles
        ArrayList<Callable<Vector<WikipediaArticle>>> processors = new ArrayList<>();
        for (int i=0; i<THREAD_COUNT && (i * BATCH_SIZE) < queueSize; i++) {
            final Vector<WikipediaArticle> articles = new Vector<>(BATCH_SIZE);
            for (int j = i * BATCH_SIZE; j<(((i+1) * BATCH_SIZE)) && j<queueSize; j++) {
                articles.add(fixedQueue[j]);
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
                            numIndexable++;
                            indexTitles[article.index] = article.analysis.parsedTitle;
                            for (String link: article.getOutgoingLinks()) {
                                if (incomingLinkMap.containsKey(link)) {
                                    int count = incomingLinkMap.get(link);
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
                if ("il".equals(typeAttribute.type()) && !outgoingLinks.contains(termAttribute.toString())) {
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

    void processQueue() {
        if ("analyze".equals(mode)) {
            this.processAnalysisQueue();
        } else {
            this.processIndexQueue();
        }
        for (int i=0; i<THREAD_COUNT * BATCH_SIZE; i++) {
            fixedQueue[i] = null;
        }
        queueSize = 0;
        if ("analyze".equals(mode)) {
            System.out.println("Analyzed articles\t[" + numLastTotal + " - " + numTotal + "]");
        } else {
            System.out.println("Indexed articles\t[" + numTotal + " / " + numAnalyzed + "]");
        }
        numLastTotal = numTotal;
    }

    void processIndexQueue() {
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
    }

    Integer indexArticles (Vector<WikipediaArticle> articles) throws IOException {
        int indexed = 0;
        for (WikipediaArticle article: articles) {
            //Get title
            String indexTitle = indexTitles[article.index];
            if (indexTitle != null) {
                //Check incoming links
                if (incomingLinkMap.containsKey(indexTitle) && incomingLinkMap.get(indexTitle) > 0) {
                    indexDocument(article.title, article.text);
                    indexed++;
                }
            }
        }
        return indexed;
    }

    void indexDocument(String title, String wikiText) throws IOException {
        if (useStanfordLemmas) {
            wikiText = getStanfordLemmatizedArticle(wikiText);
        }

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
