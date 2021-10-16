package com.dreamcloud.esa.indexer;

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

import com.dreamcloud.esa.analyzer.WikiLinkAnalyzer;
import com.dreamcloud.esa.analyzer.WikipediaArticle;
import com.dreamcloud.esa.analyzer.WikipediaArticleAnalysis;
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
    private static String TEXT_FIELD = "text";
    private static String TITLE_FIELD = "title";
    private final SAXParserFactory saxFactory;
    private ExecutorService executorService;
    WikipediaArticle[] fixedQueue;
    int queueSize = 0;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();
    private String wikiTitle;
    private int numCurrent = 0;
    private int numLastCurrent = 0;
    private int numAnalyzed = 0;
    private int numIndexed = 0;

    private String mode;
    ArrayList<Pattern> titleExclusionPatterns;
    IndexWriter indexWriter;
    Analyzer analyzer;

    String[] indexTitles;
    MutableObjectIntMap<String> incomingLinkMap = ObjectIntMaps.mutable.empty();
    WikiIndexerOptions options;

    public WikiIndexer(WikiIndexerOptions options) {
        this.options = options;
        this.fixedQueue = new WikipediaArticle[options.threadCount * options.batchSize];
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);

        this.titleExclusionPatterns = new ArrayList<>();
        if (options.titleExclusionRegExList != null) {
            for(String titleExclusionRegEx: options.titleExclusionRegExList) {
                this.titleExclusionPatterns.add(Pattern.compile(titleExclusionRegEx));
            }
        }
    }

    public boolean requiresAnalysis() {
        return this.options.maximumTermCount > 0 || this.options.minimumTermCount > 0 || this.options.minimumIncomingLinks > 0 || this.options.minimumOutgoingLinks > 0;
    }

    void reset() {
        numCurrent = 0;
        numLastCurrent = 0;
        inPage = false;
        inPageTitle = false;
        inPageText = false;
        content = new StringBuilder();
        wikiTitle = null;
    }

    protected void analyze(File file) {
        if (this.requiresAnalysis()) {
            this.indexTitles = new String[options.maximumDocumentCount];
            reset();
            executorService = Executors.newFixedThreadPool(options.threadCount);

           analyzer = new WikiLinkAnalyzer();

            mode = "analyze";
            parseXmlDump(file);
            //There may be queue items left over
            if (queueSize > 0) {
                this.processQueue();
            }

            executorService.shutdown();
            try {
                executorService.awaitTermination(12, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.gc();
            analyzer.close();

            System.out.println("----------------------------------------");
            System.out.println("Articles Analyzed:\t" + numAnalyzed);
            System.out.println("----------------------------------------");
        }
    }

    public void index(File file) throws IOException {
        if (this.requiresAnalysis()) {
            analyze(file);
        }

        reset();
        executorService = Executors.newFixedThreadPool(options.threadCount);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(options.analyzerFactory.getAnalyzer());
        indexWriter = new IndexWriter(options.indexDirectory, indexWriterConfig);
        mode = "index";
        parseXmlDump(file);
        //There may be queue items left over
        if (queueSize > 0) {
            this.processQueue();
        }

        indexWriter.commit();
        executorService.shutdown();
        try {
            executorService.awaitTermination(12, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Analyzed:\t" + numAnalyzed);
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
            if ("analyze".equals(mode)) {
                numAnalyzed++;
            }

            numCurrent++;
            inPageText = false;
            String wikiTitleCopy = wikiTitle;
            String wikiText = content.toString();

            //Exclude specific titles ("List of" or "discography" for example)
            if (options.titleExclusionList != null) {
                for(String titleExclusion: options.titleExclusionList) {
                    if (wikiTitle.contains(titleExclusion)) {
                        return;
                    }
                }
            }

            //Exclude titles by regex
            for (Pattern pattern: this.titleExclusionPatterns) {
                Matcher matcher = pattern.matcher(wikiTitle);
                if (matcher.find()) {
                    return;
                }
            }

            fixedQueue[queueSize++] = new WikipediaArticle(numCurrent, wikiTitleCopy, wikiText);
            if (queueSize == options.batchSize * options.threadCount) {
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
        for (int i=0; i<options.threadCount && (i * options.batchSize) < queueSize; i++) {
            final Vector<WikipediaArticle> articles = new Vector<>(options.batchSize);
            for (int j = i * options.batchSize; j<(((i+1) * options.batchSize)) && j<queueSize; j++) {
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
                        if (!article.canIndex(options)) {
                            continue;
                        } else {
                            indexTitles[article.index] = article.analysis.parsedTitle;

                            if (options.minimumIncomingLinks > 0) {
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
        for (int i=0; i<options.threadCount * options.batchSize; i++) {
            fixedQueue[i] = null;
        }
        queueSize = 0;
        if ("analyze".equals(mode)) {
            System.out.println("Analyzed articles\t[" + numLastCurrent + " - " + numCurrent + "]");
        } else {
            System.out.println("Indexed articles\t[" + numCurrent + " / " + numAnalyzed + "]");
        }
        numLastCurrent = numCurrent;
    }

    void processIndexQueue() {
        //Spawn up THREAD_COUNT threads and give each BATCH_SIZE articles
        ArrayList<Callable<Integer>> processors = new ArrayList<>();
        for (int i=0; i<options.threadCount && (i * options.batchSize) < queueSize; i++) {
            Vector<WikipediaArticle> articles = new Vector<>(options.batchSize);
            for (int j = i * options.batchSize; j<(((i+1) * options.batchSize)) && j<queueSize; j++) {
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

    Integer indexArticles (Vector<WikipediaArticle> articles) throws Exception {
        int indexed = 0;
        for (WikipediaArticle article: articles) {
            if (this.requiresAnalysis()) {
                String indexTitle = indexTitles[article.index];
                if (indexTitle == null) {
                    continue;
                }

                if (options.minimumIncomingLinks > 0) {
                    if ((!incomingLinkMap.containsKey(indexTitle) || incomingLinkMap.get(indexTitle) < options.minimumIncomingLinks)) {
                        continue;
                    }
                }
            }

            indexDocument(article.title, article.text);
            indexed++;
        }
        return indexed;
    }

    void indexDocument(String title, String wikiText) throws Exception {
        if (options.preprocessor != null) {
            wikiText = options.preprocessor.process(wikiText);
        }

        Document doc = new Document();
        doc.add(new StoredField(TITLE_FIELD, title));
        doc.add(new TextField(TEXT_FIELD, wikiText, Field.Store.NO));
        indexWriter.addDocument(doc);
    }

    public void close() throws IOException {
        indexWriter.close();
    }
}
