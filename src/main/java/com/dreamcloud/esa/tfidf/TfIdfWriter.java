package com.dreamcloud.esa.tfidf;

import com.dreamcloud.esa.analyzer.WikipediaArticle;
import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.database.TfIdfScoreRepository;
import com.dreamcloud.esa.indexer.Indexer;
import com.dreamcloud.esa.indexer.WikiIndexerOptions;
import com.dreamcloud.esa.tools.BZipFileReader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TfIdfWriter extends XmlReadingHandler implements Indexer {
    protected final SAXParserFactory saxFactory;
    private ExecutorService executorService;
    WikipediaArticle[] fixedQueue;
    WikipediaArticle article;
    int queueSize = 0;
    private int numIndexed = 0;
    WikiIndexerOptions options;
    boolean analyzed = false;
    final TfIdfAnalyzer tfIdfAnalyzer;

    public TfIdfWriter(WikiIndexerOptions options) {
        this.options = options;
        this.fixedQueue = new WikipediaArticle[options.threadCount * options.batchSize];
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
        tfIdfAnalyzer = new TfIdfAnalyzer(options.analyzerFactory.getAnalyzer(), "1tc");
    }

    public void reset() {
        super.reset();
        numIndexed = 0;
        queueSize = 0;
        article = null;
        this.fixedQueue = new WikipediaArticle[options.threadCount * options.batchSize];
    }

    public void index(File file) throws IOException {
        reset();
        executorService = Executors.newFixedThreadPool(options.threadCount);
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

        if (analyzed == false) {
            //Write the document frequencies
            TfIdfScoreRepository tfIdfScoreRepository = new TfIdfScoreRepository();
            tfIdfScoreRepository.saveTermDocumentFrequencies(tfIdfAnalyzer.getDocumentFrequencies());
        }

        analyzed = !analyzed;

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Indexed:\t" + numIndexed);
        System.out.println("Articles Skipped:\t" + (this.getDocsRead() - numIndexed));
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Acceptance Rate:\t" + format.format(((double) numIndexed) / ((double) getDocsRead())));
        System.out.println("----------------------------------------");
    }

    public void parseXmlDump(File file) {
        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            Reader reader = BZipFileReader.getFileReader(file);
            InputSource is = new InputSource(reader);
            is.setEncoding("UTF-8");
            saxParser.parse(is, this);
            reader.close();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(com.dreamcloud.esa.indexer.WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    void processQueue() {
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

        for (int i=0; i<options.threadCount * options.batchSize; i++) {
            fixedQueue[i] = null;
        }
        queueSize = 0;
        System.out.println("Indexed articles\t[" + numIndexed + " | " + getDocsRead() + "]");
    }

    Integer indexArticles (Vector<WikipediaArticle> articles) throws Exception {
        TfIdfScoreRepository scoreRepository = new TfIdfScoreRepository();
        for (WikipediaArticle article: articles) {
            indexDocument(article, scoreRepository);
        }
        return articles.size();
    }

    void indexDocument(WikipediaArticle article, TfIdfScoreRepository scoreRepository) throws Exception {
        String wikiText = article.text;
        if (options.preprocessor != null) {
            wikiText = options.preprocessor.process(wikiText);
        }

        if (!analyzed) {
            tfIdfAnalyzer.prepareDocument(wikiText);
        } else {
            TfIdfScore[] scores = tfIdfAnalyzer.getTfIdfScores(wikiText);
            scoreRepository.saveTfIdfScores(article.title, scores);
        }
    }

    public void close() throws IOException {
    }

    protected void handleDocument(Map<String, String> xmlFields) throws SAXException {
        WikipediaArticle article = new WikipediaArticle();
        article.title = xmlFields.get("title");
        article.text = xmlFields.get("text");
        article.incomingLinks = Integer.parseInt(xmlFields.get("incomingLinks"));
        article.outgoingLinks = Integer.parseInt(xmlFields.get("outgoingLinks"));
        article.terms = Integer.parseInt(xmlFields.get("terms"));

        if (article.canIndex(options)) {
            fixedQueue[queueSize++] = article;
        }

        if (queueSize == options.batchSize * options.threadCount) {
            this.processQueue();
            queueSize = 0;
        }
    }
}
