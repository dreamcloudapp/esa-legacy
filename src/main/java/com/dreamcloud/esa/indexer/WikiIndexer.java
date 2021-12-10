package com.dreamcloud.esa.indexer;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.dreamcloud.esa.analyzer.TrueBM25Similarity;
import com.dreamcloud.esa.analyzer.WikipediaArticle;
import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.similarity.TrueTFIDFSimilarity;
import com.dreamcloud.esa.tools.BZipFileReader;
import com.dreamcloud.esa.tools.StringUtils;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Philip van Oosten
 */
public class WikiIndexer extends XmlReadingHandler implements Indexer {
    protected final SAXParserFactory saxFactory;
    private ExecutorService executorService;
    WikipediaArticle[] fixedQueue;
    WikipediaArticle article;
    int queueSize = 0;
    private int numIndexed = 0;
    static Pattern linkRegexPattern = Pattern.compile("\\[\\[(?!File:|Image:)([^|#\\]]+)[^]]*]]");

    IndexWriter indexWriter;
    WikiIndexerOptions options;

    public WikiIndexer(WikiIndexerOptions options) {
        this.options = options;
        this.fixedQueue = new WikipediaArticle[options.threadCount * options.batchSize];
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
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
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(options.analyzerFactory.getAnalyzer());
        //indexWriterConfig.setSimilarity(new TrueTFIDFSimilarity());
        indexWriter = new IndexWriter(options.indexDirectory, indexWriterConfig);
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
            Logger.getLogger(WikiIndexer.class.getName()).log(Level.SEVERE, null, ex);
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
        for (WikipediaArticle article: articles) {
            indexDocument(article.title, article.text);
        }
        return articles.size();
    }

    void indexDocument(String title, String wikiText) throws Exception {
        if (options.preprocessor != null) {
            wikiText = options.preprocessor.process(wikiText);
        }

        Document doc = new Document();
        doc.add(new StoredField("title", title));
        doc.add(new TextField("text", wikiText, Field.Store.NO));
        Matcher matcher = linkRegexPattern.matcher(wikiText);
        while (matcher.find()) {
            String normalizedLink = StringUtils.normalizeWikiTitle(matcher.group(1));
            doc.add(new StoredField("outgoingLink", normalizedLink));
        }
        indexWriter.addDocument(doc);
    }

    public void close() throws IOException {
        indexWriter.close();
    }

    public void handleDocument(Map<String, String> xmlFields) throws SAXException {
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
