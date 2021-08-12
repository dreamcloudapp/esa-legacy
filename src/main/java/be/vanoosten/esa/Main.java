package be.vanoosten.esa;

import static be.vanoosten.esa.WikiIndexer.TITLE_FIELD;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Arrays;
import java.util.List;

import be.vanoosten.esa.tools.ConceptVector;
import jdk.internal.org.jline.reader.impl.DefaultParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.BytesRef;

import org.apache.commons.cli.*;

import be.vanoosten.esa.tools.SemanticSimilarityTool;
import be.vanoosten.esa.tools.Vectorizer;

import static org.apache.lucene.util.Version.LUCENE_48;

//Reading input files
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author Philip van Oosten
 */
public class Main {

    public static String readInputFile(String path, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static Boolean nonEmpty(String s) {
        return s != null && !s.equals("");
    }

    public static Boolean hasLength(String[] a, int length) {
        return a != null && a.length == length;
    }

    public static void main(String[] args) throws IOException, ParseException {
        WikiFactory factory = new EnwikiFactory();
        CharArraySet stopWords = factory.getStopWords();
        DecimalFormat decimalFormat = new DecimalFormat("#.00");

        Options options = new Options();
        Option compareTextOption = new Option("ct", "compare-texts", true, "comparison text");
        compareTextOption.setRequired(false);
        compareTextOption.setArgs(2);
        options.addOption(compareTextOption);

        Option compareFileOption = new Option("cf", "compare-files", true, "comparison files");
        compareFileOption.setRequired(false);
        compareFileOption.setArgs(2);
        options.addOption(compareFileOption);

        Option topTextOption = new Option("tt", "top-text", true, "top concepts for text");
        topTextOption.setRequired(false);
        options.addOption(topTextOption);

        Option topFileOption = new Option("tf", "top-text", true, "top concepts for file");
        topFileOption.setRequired(false);
        options.addOption(topFileOption);

        Option limitOption = new Option("l", "limit", true, "concept query limit");
        limitOption.setRequired(false);
        options.addOption(limitOption);

        //Indexing
        Option indexOption = new Option("i", "index", true, "index a wikipedia dump bz2 file");
        indexOption.setRequired(false);
        options.addOption(indexOption);

        Option mapOption = new Option("m", "map", false, "map terms to concepts");
        mapOption.setRequired(false);
        options.addOption(mapOption);

        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);
            String[] compareTexts = cmd.getOptionValues("ct");
            String[] compareFiles = cmd.getOptionValues("cf");
            String[] topText = cmd.getOptionValues("tt");
            String[] topFile = cmd.getOptionValues("tf");
            String limit = cmd.getOptionValue("l");
            String index = cmd.getOptionValue("i");

            //Comparison of texts
            if (hasLength(compareTexts, 2) || hasLength(compareFiles, 2)) {
                String sourceText;
                String compareText;

                if (hasLength(compareTexts, 2)) {
                    sourceText = compareTexts[0];
                    compareText = compareTexts[1];
                } else {
                    sourceText = readInputFile(compareTexts[0], "utf-8");
                    compareText = readInputFile(compareTexts[1], "utf-8");
                }
               String sourceDesc = sourceText.substring(0, Math.min(16, sourceText.length()));
                if (sourceText.length() > 16) {
                    sourceDesc += "...";
                }
               String compareDesc = compareText.substring(0, Math.min(16, compareText.length()));
                if (compareText.length() > 16) {
                    compareDesc += "...";
                }
               System.out.println("Comparing '" + sourceDesc + "' to '" + compareDesc + "':");
                WikiAnalyzer analyzer = new WikiAnalyzer(LUCENE_48, stopWords);
                Vectorizer vectorizer = new Vectorizer(new File("./index/conceptterm"), analyzer);
                if (nonEmpty(limit)) {
                    try {
                        Integer conceptCount = Integer.parseInt(limit);
                        vectorizer.setConceptCount(conceptCount);
                    } catch (NumberFormatException e) {

                    }
                }
                System.out.println("Limiting to top " + vectorizer.getConceptCount() + " concepts per document.");
                SemanticSimilarityTool similarity = new SemanticSimilarityTool(vectorizer);
                System.out.println("Vector relatedness: " + decimalFormat.format(similarity.findSemanticSimilarity(sourceText, compareText))
                );
            }

            //Top concepts
            else if (hasLength(topText, 1) || hasLength(topFile, 1)) {
                Integer topConcepts = 10;
                try {
                    topConcepts = Integer.parseInt(limit);
                } catch (NumberFormatException e) {

                }
                String sourceText;
                if (hasLength(topText, 1)) {
                    sourceText = topText[0];
                } else {
                    sourceText = readInputFile(topFile[0], "utf-8");
                }
                String sourceDesc = sourceText.substring(0, Math.min(16, sourceText.length()));
                if (sourceText.length() > 16) {
                    sourceDesc += "...";
                }
                System.out.println("Getting top " + topConcepts + " concepts for '" + sourceDesc + "':");
                WikiAnalyzer analyzer = new WikiAnalyzer(LUCENE_48, stopWords);
                Vectorizer vectorizer = new Vectorizer(new File("./index/conceptterm"), analyzer);
                vectorizer.setConceptCount(topConcepts);
                ConceptVector vector = vectorizer.vectorize(sourceText);
                Iterator<String> topTenConcepts = vector.topConcepts(topConcepts);
                for (Iterator<String> it = topTenConcepts; it.hasNext(); ) {
                    String concept = it.next();
                    System.out.println(concept + ": " + decimalFormat.format(vector.getConceptWeights().get(concept)));
                }
            }

            //Index a dump file
            else if (nonEmpty(index)) {
                System.out.println("Indexing " + index + "...");
                File wikipediaDumpFile = new File(index);
                indexing(new File("./index/termdoc"), wikipediaDumpFile, stopWords);
                System.out.println("Created index at 'index/termdoc'.");
            }

            //Map the terms to concepts
            else if (cmd.hasOption("m")) {
                createConceptTermIndex(new File("./index/termdoc"), new File("./index/conceptterm"));
                System.out.println("Created index at 'index/conceptterm'.");
            } else {
                formatter.printHelp("wiki-esa", options);
            }

        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("wiki-esa", options);
            System.exit(1);
        }
    }

    /**
     * Creates a concept-term index from a term-to-concept index (a full text index of a Wikipedia dump).
     * @param termDocIndexDirectory The directory that contains the term-to-concept index, which is created by {@code indexing()} or in a similar fashion.
     * @param conceptTermIndexDirectory The directory that shall contain the concept-term index.
     * @throws IOException
     */
    static void createConceptTermIndex(File termDocIndexDirectory, File conceptTermIndexDirectory) throws IOException {
        ExecutorService es = Executors.newFixedThreadPool(8); //2 to 8.

        final Directory termDocDirectory = FSDirectory.open(termDocIndexDirectory);
        final IndexReader termDocReader = IndexReader.open(termDocDirectory);
        final IndexSearcher docSearcher = new IndexSearcher(termDocReader);

        Fields fields = MultiFields.getFields(termDocReader);
        if (fields != null) {
            Terms terms = fields.terms(WikiIndexer.TEXT_FIELD);
            TermsEnum termsEnum = terms.iterator(null);

            final IndexWriterConfig conceptIndexWriterConfig = new IndexWriterConfig(LUCENE_48, null);
            try (IndexWriter conceptIndexWriter = new IndexWriter(FSDirectory.open(conceptTermIndexDirectory), conceptIndexWriterConfig)) {
                int t = 0;
                BytesRef bytesRef;
                while ((bytesRef = termsEnum.next()) != null) {
                    String termString = bytesRef.utf8ToString();
                    if (termString.matches("^[a-zA-Z]+:/.*$") || termString.matches("^\\d+$")) {
                        continue;
                    }
                    if (termString.charAt(0) >= '0' && termString.charAt(0) <= '9') {
                        continue;
                    }
                    if (termString.contains(".") || termString.contains("_")) {
                        continue;
                    }
                    if (t++ == 1000) {
                        t = 0;
                        System.out.println(termString);
                    }
                    TopDocs td = SearchTerm(bytesRef, docSearcher);

                    // add the concepts to the token stream
                    byte[] payloadBytes = new byte[5];
                    ByteArrayDataOutput dataOutput = new ByteArrayDataOutput(payloadBytes);
                    CachingTokenStream pcTokenStream = new CachingTokenStream();
                    double norm = ConceptSimilarity.SIMILARITY_FACTOR;
                    int last = 0;
                    for(ScoreDoc scoreDoc : td.scoreDocs){
                        if(scoreDoc.score/norm < ConceptSimilarity.SIMILARITY_FACTOR ||
                                last>= 1.0f / ConceptSimilarity.SIMILARITY_FACTOR) break;
                        norm += scoreDoc.score * scoreDoc.score;
                        last++;
                    }
                    for (int i=0; i<last; i++) {
                        ScoreDoc scoreDoc = td.scoreDocs[i];
                        Document termDocDocument = termDocReader.document(scoreDoc.doc);
                        String concept = termDocDocument.get(WikiIndexer.TITLE_FIELD);
                        Token conceptToken = new Token(concept, i * 10, (i + 1) * 10, "CONCEPT");
                        // set similarity score as payload
                        int integerScore = (int) ((scoreDoc.score/norm)/ConceptSimilarity.SIMILARITY_FACTOR);
                        dataOutput.reset(payloadBytes);
                        dataOutput.writeVInt(integerScore);
                        BytesRef payloadBytesRef = new BytesRef(payloadBytes, 0, dataOutput.getPosition());
                        conceptToken.setPayload(payloadBytesRef);
                        pcTokenStream.produceToken(conceptToken);
                    }

                    Document conceptTermDocument = new Document();
                    AttributeSource attributeSource = termsEnum.attributes();
                    conceptTermDocument.add(new StringField(WikiIndexer.TEXT_FIELD, termString, Field.Store.YES));
                    conceptTermDocument.add(new TextField("concept", pcTokenStream));
                    conceptIndexWriter.addDocument(conceptTermDocument);
                }
            }
        }
    }

    private static TopDocs SearchTerm(BytesRef bytesRef, IndexSearcher docSearcher) throws IOException {
        Term term = new Term(WikiIndexer.TEXT_FIELD, bytesRef);
        Query query = new TermQuery(term);
        int n = 1000;
        TopDocs td = docSearcher.search(query, n);
        if (n < td.totalHits) {
            n = td.totalHits;
            td = docSearcher.search(query, n);
        }
        return td;
    }

    private static void searchForQuery(final QueryParser parser, final IndexSearcher searcher, final String queryString, final IndexReader indexReader) throws ParseException, IOException {
        Query query = parser.parse(queryString);
        TopDocs topDocs = searcher.search(query, 12);
        System.out.println(String.format("%d hits voor \"%s\"", topDocs.totalHits, queryString));
        for (ScoreDoc sd : topDocs.scoreDocs) {
            System.out.println(String.format("doc %d score %.2f shardIndex %d title \"%s\"", sd.doc, sd.score, sd.shardIndex, indexReader.document(sd.doc).get(TITLE_FIELD)));
        }
    }

    /**
     * Creates a term to concept index from a Wikipedia article dump.
     * @param termDocIndexDirectory The directory where the term to concept index must be created
     * @param wikipediaDumpFile The Wikipedia dump file that must be read to create the index
     * @param stopWords The words that are not used in the semantic analysis
     * @throws IOException
     */
    public static void indexing(File termDocIndexDirectory, File wikipediaDumpFile, CharArraySet stopWords) throws IOException {
        try (Directory directory = FSDirectory.open(termDocIndexDirectory)) {
            Analyzer analyzer = new WikiAnalyzer(LUCENE_48, stopWords);
            try(WikiIndexer indexer = new WikiIndexer(analyzer, directory)){
                indexer.parseXmlDump(wikipediaDumpFile);
            }
        }
    }
}
