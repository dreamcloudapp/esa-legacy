package be.vanoosten.esa;

import static be.vanoosten.esa.WikiIndexer.TEXT_FIELD;
import static be.vanoosten.esa.WikiIndexer.TITLE_FIELD;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import be.vanoosten.esa.tools.*;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteArrayDataOutput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.commons.cli.*;
import static org.apache.lucene.util.Version.LUCENE_48;
import io.javalin.Javalin;

//Reading input files
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Philip van Oosten
 */
public class Main {
    static int THREAD_COUNT = 16;

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
        CharArraySet stopWords = EnwikiFactory.getExtendedStopWords();
        DecimalFormat decimalFormat = new DecimalFormat("#.000");
        Options options = new Options();
        Option compareTextOption = new Option("ct", "compare-texts", true, "\"string one\" \"string two\" / Compare two texts.");
        compareTextOption.setRequired(false);
        compareTextOption.setArgs(2);
        options.addOption(compareTextOption);

        Option compareFileOption = new Option("cf", "compare-files", true, "source1.txt source2.txt / Compare texts from files.");
        compareFileOption.setRequired(false);
        compareFileOption.setArgs(2);
        options.addOption(compareFileOption);

        Option topTextOption = new Option("tt", "top-text", true, "\"string\" / Get the top concepts for text.");
        topTextOption.setRequired(false);
        options.addOption(topTextOption);

        Option topFileOption = new Option("tf", "top-file", true, "input.txt / Get the top concepts for text in a file.");
        topFileOption.setRequired(false);
        options.addOption(topFileOption);

        Option docTypeOption = new Option("doctype", "doctype", true, "string / The document type (article|dream). Defaults to article.");
        docTypeOption.setRequired(false);
        options.addOption(docTypeOption);

        Option limitOption = new Option("l", "limit", true, "int / The maximum number of concepts to query when comparing texts and finding top concepts.");
        limitOption.setRequired(false);
        options.addOption(limitOption);

        Option similarityOption = new Option("similarity", "similarity", true, "[tfidf|bm25] / The scoring similarity algorithm, defaulting to tfidf.");
        similarityOption.setRequired(false);
        options.addOption(similarityOption);

        Option vectorizerOption = new Option("vectorizer", "vectorizer", true, "[standard|narrative] / The vectorizing algorithm to use, defaulting to standard.");
        vectorizerOption.setRequired(false);
        options.addOption(vectorizerOption);

        Option cohesionOption = new Option("cohesion", "cohesion", true, "float / The cohesion for grouping sentences into topics.");
        cohesionOption.setRequired(false);
        options.addOption(cohesionOption);

        //Debugging
        Option debugOption = new Option("d", "debug", true, "input.txt / Shows the tokens for a text.");
        debugOption.setRequired(false);
        options.addOption(debugOption);

        //Indexing
        Option indexOption = new Option("i", "index", true, "dump.bz2 / Indexes a Wikipedia XML dump file in bz2 format.");
        indexOption.setRequired(false);
        options.addOption(indexOption);

        Option mapOption = new Option("m", "map", false, "Maps terms to concepts. Must run the index first.");
        mapOption.setRequired(false);
        options.addOption(mapOption);

        Option testOption = new Option("t", "test", false, "Performs test comparisons on a set of dreams and news sources.");
        testOption.setRequired(false);
        options.addOption(testOption);

        Option indexMapOption = new Option("im", "index-map", true, "Indexes and maps together.");
        indexMapOption.setRequired(false);
        options.addOption(indexMapOption);

        //Server stuff
        Option serverOption = new Option("server", "server", true, "port / Starts a vectorizing server using the specified port.");
        serverOption.setRequired(false);
        options.addOption(serverOption);

        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);
            String[] compareTexts = cmd.getOptionValues("ct");
            String[] compareFiles = cmd.getOptionValues("cf");
            String[] topText = cmd.getOptionValues("tt");
            String[] topFile = cmd.getOptionValues("tf");

            String docType = cmd.getOptionValue("doctype");
            if (!nonEmpty(docType)) {
                docType = "article";
            }
            String termDoc = docType + "_" + "termdoc";
            //Need to clean this up
            WikiFactory.docType = docType;

            String limit = cmd.getOptionValue("l");
            int conceptLimit = 1000;
            if (nonEmpty(limit)) {
                try {
                    conceptLimit = Integer.parseInt(limit);
                } catch (NumberFormatException e) {

                }
            }

            String similarity = cmd.getOptionValue("similarity");
            if (nonEmpty(similarity)) {
                SimilarityFactory.setSimilarityType(similarity);
            }

            String vectorizer = cmd.getOptionValue("vectorizer");
            String cohesion = cmd.getOptionValue("cohesion");
            double cohesionValue = 0.15;
            if (nonEmpty(cohesion)) {
                try {
                    cohesionValue = Double.parseDouble(cohesion);
                } catch (NumberFormatException e) {

                }
            }

            VectorizerFactory vectorizerFactory = new VectorizerFactory(vectorizer, conceptLimit, cohesionValue);

            String server = cmd.getOptionValue("server");
            String debug = cmd.getOptionValue("d");
            String index = cmd.getOptionValue("i");
            String indexMap = cmd.getOptionValue("im");

            //Get the unixtime
            long startTime = Instant.now().getEpochSecond();

            //Comparison of texts
            if (hasLength(compareTexts, 2) || hasLength(compareFiles, 2)) {
                String sourceText;
                String compareText;

                if (hasLength(compareTexts, 2)) {
                    sourceText = compareTexts[0];
                    compareText = compareTexts[1];
                } else {
                    sourceText = readInputFile(compareFiles[0], "utf-8");
                    compareText = readInputFile(compareFiles[1], "utf-8");
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
                TextVectorizer textVectorizer = vectorizerFactory.getTextVectorizer();
                SemanticSimilarityTool similarityTool = new SemanticSimilarityTool(textVectorizer);
                System.out.println("Vector relatedness: " + decimalFormat.format(similarityTool.findSemanticSimilarity(sourceText, compareText))
                );
            }

            //Top concepts
            else if (hasLength(topText, 1) || hasLength(topFile, 1)) {
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
                TextVectorizer textVectorizer = vectorizerFactory.getTextVectorizer();
                ConceptVector vector = textVectorizer.vectorize(sourceText);
                Iterator<String> topTenConcepts = vector.topConcepts();
                for (Iterator<String> it = topTenConcepts; it.hasNext(); ) {
                    String concept = it.next();
                    System.out.println(concept + ": " + decimalFormat.format(vector.getConceptWeights().get(concept)));
                }
            }

            //Debug tokens
            else if(nonEmpty(debug)) {
                String sourceText = readInputFile(debug, "utf-8");
                String sourceDesc = sourceText.substring(0, Math.min(16, sourceText.length()));
                if (sourceText.length() > 16) {
                    sourceDesc += "...";
                }
                System.out.println("Debugging '" + sourceDesc + "':");
                TokenStream ts = AnalyzerFactory.getVectorizingAnalyzer().tokenStream(TEXT_FIELD, sourceText);
                CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
                TypeAttribute typeAttribute = ts.addAttribute(TypeAttribute.class);

                try{
                    ts.reset();
                    while (ts.incrementToken()) {
                        System.out.println(typeAttribute.type() + ": " + charTermAttribute);
                    }
                    ts.end();
                } finally {
                    ts.close();
                }
            }

            else if(cmd.hasOption("t")) {
                //Get test files
                File dir = new File("./src/test/data");
                File[] files = dir.listFiles();
                ArrayList<ComparisonFile> comparisonFiles = new ArrayList<>();
                for(File file: files) {
                    comparisonFiles.add(new ComparisonFile(file.getName(), readInputFile(file.getPath(), "utf-8")));
                }

                //Setup
                TextVectorizer textVectorizer = vectorizerFactory.getTextVectorizer();
                SemanticSimilarityTool similarityTool = new SemanticSimilarityTool(textVectorizer);

                System.out.println("Testing dream and news comparisons...");
                System.out.println("----------------------------------------");
                while(comparisonFiles.size() > 1) {
                    ComparisonFile file = comparisonFiles.remove(0);
                    for (ComparisonFile comparisonFile: comparisonFiles) {
                        //Compare the two and show results
                        System.out.println(file.name + "_" + comparisonFile.name + ": " + decimalFormat.format(similarityTool.findSemanticSimilarity(file.text, comparisonFile.text)));
                    }
                }
            }

            //Indexing and mapping
            else if(nonEmpty(indexMap) || nonEmpty(index) || cmd.hasOption("m")) {
                if (nonEmpty(indexMap) || nonEmpty(index)) {
                    String fileName = nonEmpty(indexMap) ? indexMap : index;
                    System.out.println("Indexing " + fileName + "...");
                    File wikipediaDumpFile = new File(fileName);
                    indexing(new File("./index/" + termDoc), wikipediaDumpFile, stopWords, docType);
                    System.out.println("Created index at 'index/" + termDoc + "'.");
                }

                if (nonEmpty(indexMap)) {
                    System.out.println("");
                }

                if (nonEmpty(indexMap) || cmd.hasOption("m")) {
                    System.out.println("Mapping terms to concepts...");
                    createConceptTermIndex(new File("./index/" + termDoc), new File("./index/wiki_conceptterm"));
                    System.out.println("Created index at 'index/wiki_conceptterm'.");
                }
            } else if(nonEmpty(server)) {
                TextVectorizer textVectorizer = vectorizerFactory.getTextVectorizer();
                int port = 1994;
                try {
                    port = Integer.parseInt(server);
                } catch (NumberFormatException e) {

                }
                Javalin app = Javalin.create().start(port);
                app.post("/vectorize", ctx -> {
                    String text = ctx.body();
                    if ("".equals(text)) {
                        ctx.res.sendError(400, "Invalid input text.");
                    } else {
                        ConceptVector vector = textVectorizer.vectorize(text);
                        ctx.json(vector.getConceptWeights());
                        System.out.println("Processed dream: " + text.substring(0, 16) + "...");
                    }
                });
            }
            else {
                formatter.printHelp("wiki-esa", options);
            }

            long endTime = Instant.now().getEpochSecond();
            System.out.println("----------------------------------------");
            System.out.println("Process finished in " + (endTime - startTime) + " seconds.");
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("wiki-esa", options);
            System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a concept-term index from a term-to-concept index (a full text index of a Wikipedia dump).
     * @param termDocIndexDirectory The directory that contains the term-to-concept index, which is created by {@code indexing()} or in a similar fashion.
     * @param conceptTermIndexDirectory The directory that shall contain the concept-term index.
     */
    static void createConceptTermIndex(File termDocIndexDirectory, File conceptTermIndexDirectory) throws IOException {
        final Directory termDocDirectory = FSDirectory.open(termDocIndexDirectory);
        final IndexReader termDocReader = IndexReader.open(termDocDirectory);
        final IndexSearcher docSearcher = new IndexSearcher(termDocReader);

        Fields fields = MultiFields.getFields(termDocReader);
        if (fields != null) {
            Terms terms = fields.terms(WikiIndexer.TEXT_FIELD);

            final IndexWriterConfig conceptIndexWriterConfig = new IndexWriterConfig(LUCENE_48, null);
            try (IndexWriter conceptIndexWriter = new IndexWriter(FSDirectory.open(conceptTermIndexDirectory), conceptIndexWriterConfig)) {

                ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);

                long termCount = 0;
                TermsEnum termsIterator = terms.iterator(null);
                while(termsIterator.next() != null) {
                    termCount++;
                }
                AtomicLong termsProcessed = new AtomicLong(0);

                final long termsPerThread = termCount / THREAD_COUNT;
                System.out.println("Creating " + THREAD_COUNT + " threads each processing " + termsPerThread + " terms.");
                final long termsLeftOver = terms.size() % THREAD_COUNT;

                for (int i=0; i<THREAD_COUNT; i++) {
                    final int iCopy = i;
                    long finalTermCount = termCount;
                    executorService.submit(() -> {
                        long startTermIndex = termsPerThread * iCopy;
                        long endTermIndex = startTermIndex + termsPerThread + (iCopy == 15 ? termsLeftOver : 0);

                        System.out.println("Thread " + iCopy + " is handling terms [" + startTermIndex + ", " + (endTermIndex - 1) + "]");

                        try {
                            TermsEnum termsEnum = terms.iterator(null);
                            for (long termIndex = 0; termIndex < startTermIndex; termIndex++) {
                                termsEnum.next();
                            }

                            int t = 0;
                            BytesRef bytesRef;
                            while ((bytesRef = termsEnum.next()) != null && startTermIndex < endTermIndex) {
                                String termString = bytesRef.utf8ToString();
                                String padding = "";
                                int tabs = 4 - (termString.length() / 8);
                                while (tabs-- > 0) {
                                    padding += "\t";
                                }
                                long processed = termsProcessed.incrementAndGet();
                                if (t++ == 1000) {
                                    t = 0;
                                    System.out.println(termString + padding + "[" + processed + " / " + finalTermCount + "]");
                                }
                                TopDocs td = SearchTerm(bytesRef, docSearcher);

                                // add the concepts to the token stream
                                byte[] payloadBytes = new byte[5];
                                ByteArrayDataOutput dataOutput = new ByteArrayDataOutput(payloadBytes);
                                CachingTokenStream pcTokenStream = new CachingTokenStream();
                                double norm = ConceptSimilarity.SIMILARITY_FACTOR;
                                int last = 0;
                                for (ScoreDoc scoreDoc : td.scoreDocs) {
                                    if (scoreDoc.score / norm < ConceptSimilarity.SIMILARITY_FACTOR ||
                                            last >= 1.0f / ConceptSimilarity.SIMILARITY_FACTOR) break;
                                    norm += scoreDoc.score * scoreDoc.score;
                                    last++;
                                }
                                for (int j = 0; j < last; j++) {
                                    ScoreDoc scoreDoc = td.scoreDocs[j];
                                    Document termDocDocument = termDocReader.document(scoreDoc.doc);
                                    String concept = termDocDocument.get(WikiIndexer.TITLE_FIELD);
                                    Token conceptToken = new Token(concept, j * 10, (j + 1) * 10, "CONCEPT");
                                    // set similarity score as payload
                                    int integerScore = (int) ((scoreDoc.score / norm) / ConceptSimilarity.SIMILARITY_FACTOR);
                                    dataOutput.reset(payloadBytes);
                                    dataOutput.writeVInt(integerScore);
                                    BytesRef payloadBytesRef = new BytesRef(payloadBytes, 0, dataOutput.getPosition());
                                    conceptToken.setPayload(payloadBytesRef);
                                    pcTokenStream.produceToken(conceptToken);
                                }

                                Document conceptTermDocument = new Document();
                                conceptTermDocument.add(new StringField(WikiIndexer.TEXT_FIELD, termString, Field.Store.YES));
                                conceptTermDocument.add(new TextField("concept", pcTokenStream));
                                conceptIndexWriter.addDocument(conceptTermDocument);
                                startTermIndex++;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                try {
                    executorService.shutdown();
                    Boolean result = executorService.awaitTermination(12, TimeUnit.HOURS);
                    if (result) {
                        System.out.println("All threads successfully completed.");
                    } else {
                        System.out.println("Thread timeout exceeded.");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
    public static void indexing(File termDocIndexDirectory, File wikipediaDumpFile, CharArraySet stopWords, String docType) throws IOException {
        try (Directory directory = FSDirectory.open(termDocIndexDirectory)) {

            Indexer indexer;
            if ("article".equals(docType)) {
                indexer =  new WikiIndexer(directory);
            } else {
                indexer = new DreamIndexer(directory);
            }
            try{
                System.out.println("Analyzing the Wikipedia dump file to calculate token and link counts...");
                indexer.analyze(wikipediaDumpFile);
                System.out.println("Finished analysis.");

                System.out.println("");
                System.out.println("Writing the index...");
                indexer.index(wikipediaDumpFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
