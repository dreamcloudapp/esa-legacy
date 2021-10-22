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

import com.dreamcloud.esa.analyzer.WikipediaArticle;
import com.dreamcloud.esa.tools.BZipFileReader;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
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
    WikipediaArticle article;
    int queueSize = 0;
    private boolean inDoc;
    private String element;
    private StringBuilder content = new StringBuilder();
    private int numRead = 0;
    private int numIndexed = 0;

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

    void reset() {
        numRead = 0;
        inDoc = false;
        content = new StringBuilder();
        article = null;
        element = null;
    }

    public void index(File file) throws IOException {
        reset();
        executorService = Executors.newFixedThreadPool(options.threadCount);
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(options.analyzerFactory.getAnalyzer());
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
        System.out.println("Articles Skipped:\t" + (numRead - numIndexed));
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Acceptance Rate:\t" + format.format(((double) numIndexed) / ((double) numRead)));
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

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("doc".equals(localName)) {
            inDoc = true;
            article = new WikipediaArticle();
        } else if(inDoc) {
            element = localName;
            content = new StringBuilder();
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (inDoc && "doc".equals(localName)) {
            inDoc = false;
            //process document
            numRead++;

            if (article.canIndex(options)) {
                fixedQueue[queueSize++] = article;
            }

            if (queueSize == options.batchSize * options.threadCount) {
                this.processQueue();
                queueSize = 0;
            }
        } else if (inDoc && element != null) {
            String value = content.toString();
            switch (element) {
                case "title":
                    article.title = value;
                    break;
                case "text":
                    article.text = value;
                    break;
                case "incomingLinks":
                    article.incomingLinks = Integer.parseInt(value);
                    break;
                case "outgoingLinks":
                    article.outgoingLinks = Integer.parseInt(value);
                    break;
                case "terms":
                    article.terms =  Integer.parseInt(value);
                    break;
            }
        }
    }

    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
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
        System.out.println("Indexed articles\t[" + numRead + "]");
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
        indexWriter.addDocument(doc);
    }

    public void close() throws IOException {
        indexWriter.close();
    }
}
