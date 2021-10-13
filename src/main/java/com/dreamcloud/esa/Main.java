package com.dreamcloud.esa;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.dreamcloud.esa.analyzer.AnalyzerFactory;
import com.dreamcloud.esa.database.ConceptWeight;
import com.dreamcloud.esa.database.DocumentVector;
import com.dreamcloud.esa.database.VectorRepository;
import com.dreamcloud.esa.documentPreprocessor.ChainedPreprocessor;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessorFactory;
import com.dreamcloud.esa.indexer.DreamIndexer;
import com.dreamcloud.esa.indexer.Indexer;
import com.dreamcloud.esa.indexer.IndexerFactory;
import com.dreamcloud.esa.indexer.WikiIndexerOptions;
import com.dreamcloud.esa.server.DocumentSimilarityRequestBody;
import com.dreamcloud.esa.server.DocumentSimilarityScorer;
import com.dreamcloud.esa.server.DocumentVectorizationRequestBody;
import com.dreamcloud.esa.tools.*;
import com.dreamcloud.esa.vectorizer.ConceptVector;
import com.dreamcloud.esa.vectorizer.TextVectorizer;
import com.dreamcloud.esa.vectorizer.Vectorizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.commons.cli.*;
import io.javalin.Javalin;
import com.google.gson.Gson;

//Reading input files
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

public class Main {
    static String ODBC_ENVIRONMENT_VARIABLE = "DC_ESA_ODBC_CONNECTION_STRING";

    public static String readInputFile(String path, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static boolean nonEmpty(String s) {
        return s != null && !s.equals("");
    }

    public static boolean hasLength(String[] a, int length) {
        return a != null && a.length >= length;
    }

    public static void main(String[] args) throws IOException, ParseException {
        DecimalFormat decimalFormat = new DecimalFormat("#.000");
        Options options = new Options();

        //Main options
        Option docTypeOption = new Option("doctype", "doctype", true, "string / The document type (wiki|dream). Defaults to wiki.");
        docTypeOption.setRequired(false);
        options.addOption(docTypeOption);

        //Main action options
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

        Option weightOption = new Option("weight", "weight", true, "docId documentText / Gets the weights of terms within a document");
        weightOption.setArgs(2);
        weightOption.setRequired(false);
        options.addOption(weightOption);

        Option relevanceOption = new Option("relevance", "relevance", true, "\"term docId\" / Computes the relevance of a term to a document id.");
        relevanceOption.setArgs(2);
        relevanceOption.setRequired(false);
        options.addOption(relevanceOption);

        //Indexing Options
        Option minimumTermCountOption = new Option("min-terms", "min-terms", true, "int / (indexing)\tThe minimum number of terms allowed for a document.");
        minimumTermCountOption.setRequired(false);
        options.addOption(minimumTermCountOption);

        Option maximumTermCountOption = new Option("max-terms", "max-terms", true, "int / (indexing)\tThe maximum number of terms allowed for a document.");
        maximumTermCountOption.setRequired(false);
        options.addOption(maximumTermCountOption);

        Option threadCountOption = new Option("threads", "threads", true, "int / (indexing)\tThe number of threads to use.");
        threadCountOption.setRequired(false);
        options.addOption(threadCountOption);

        Option batchSizeOption = new Option("batch-size", "batch-size", true, "int / (indexing)\tThe number of documents to process at once, distributed across the threads.");
        batchSizeOption.setRequired(false);
        options.addOption(batchSizeOption);

        Option maximumDocumentCountOption = new Option("max-docs", "max-docs", true, "int / (indexing)\tThe maximum number of documents we can process before throwing an error (defaults to 512,000).");
        maximumDocumentCountOption.setRequired(false);
        options.addOption(maximumDocumentCountOption);

        //Wiki specific indexing options
        Option minimumIncomingLinksOption = new Option("min-incoming-links", "min-incoming-links", true, "int / (indexing:wiki)\tThe minimum number of incoming links.");
        minimumIncomingLinksOption.setRequired(false);
        options.addOption(minimumIncomingLinksOption);

        Option minimumOutgoingLinksOption = new Option("min-outgoing-links", "min-outgoing-links", true, "int / (indexing:wiki)\tThe minimum number of outgoing links.");
        minimumOutgoingLinksOption.setRequired(false);
        options.addOption(minimumOutgoingLinksOption);

        Option titleExclusionRegExListOption = new Option("title-exclusion-regex", "title-exclusion-regex", true, "string [string2 ...] / (indexing:wiki)\tA list of regexes used to exclude Wiki article titles.");
        titleExclusionRegExListOption.setRequired(false);
        options.addOption(titleExclusionRegExListOption);

        Option titleExclusionListOption = new Option("title-exclusion", "title-exclusion-regex", true, "string [string2 ...] / (indexing:wiki)\tA list of strings used to exclude Wiki article titles which contain them.");
        titleExclusionListOption.setRequired(false);
        options.addOption(titleExclusionListOption);

        //Analyzer options
        Option limitOption = new Option("vector-limit", "vector-limit", true, "int / The maximum number of entries in each document vector.");
        limitOption.setRequired(false);
        options.addOption(limitOption);

        Option stopWordsOption = new Option("stopwords", "stopwords", true, "stopwords file / A file containing stopwords each on their own line");
        stopWordsOption.setRequired(false);
        options.addOption(stopWordsOption);

        Option dictionaryOption = new Option("dictionary", "dictionary", true, "dictionary file / A file containing a list of allowed words");
        dictionaryOption.setRequired(false);
        options.addOption(dictionaryOption);

        Option preprocessorOption = new Option("preprocessor", "preprocessor", true, "preprocessor [preprocessor2 ...] / The preprocessors to apply to input and corpus texts.");
        preprocessorOption.setRequired(false);
        options.addOption(preprocessorOption);

        Option stanfordPosOption = new Option("stanford-pos", "stanford-pos", true, "pos [pos2 ...] / The parts of speech to include when using the Stanford Lemma preprocessor");
        stanfordPosOption.setRequired(false);
        options.addOption(stanfordPosOption);

        //Debugging
        Option debugOption = new Option("d", "debug", true, "input.txt / Shows the tokens for a text.");
        debugOption.setRequired(false);
        options.addOption(debugOption);

        //Indexing
        Option indexOption = new Option("i", "index", true, "input file / Indexes a corpus of documents.");
        indexOption.setRequired(false);
        options.addOption(indexOption);

        //Index path
        Option indexPathOption = new Option("index-path", "index-path", true, "input directory / The path to the input directory (defaults to ./index/$doctype)");
        indexPathOption.setRequired(false);
        options.addOption(indexPathOption);

        //Server options
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
            String[] relevanceArgs = cmd.getOptionValues("relevance");
            String[] weightArgs = cmd.getOptionValues("weight");
            String docType = cmd.getOptionValue("doctype");
            String stopWords = cmd.getOptionValue("stopwords");
            String dictionary = cmd.getOptionValue("dictionary");
            String indexPath = cmd.getOptionValue("indexPath");

            EsaOptions esaOptions = new EsaOptions();

            if (!nonEmpty(docType)) {
                docType = "article";
            }
            esaOptions.documentType = DocumentType.valueOfLabel(docType);
            if ( esaOptions.documentType == null) {
                throw new IllegalArgumentException("Document type " + docType + " is not recognized.");
            }
            esaOptions.indexPath = Paths.get(nonEmpty(indexPath) ? indexPath : "./index/" + docType + "_index");


            String limit = cmd.getOptionValue("vector-limit");
            int documentLimit = 100;
            if (nonEmpty(limit)) {
                try {
                    documentLimit = Integer.parseInt(limit);
                } catch (NumberFormatException e) {

                }
            }
            esaOptions.documentLimit = documentLimit;

            if (nonEmpty(stopWords)) {
                if (stopWords.equals("en-default")) {
                    stopWords = "./src/data/en-stopwords.txt";
                }
                esaOptions.stopWordRepository = new StopWordRepository(stopWords);
            } else {
                esaOptions.stopWordRepository = new StopWordRepository();
            }

            if (nonEmpty(dictionary)) {
                if (dictionary.equals("en-default")) {
                    dictionary = "./src/data/en-words.txt";
                }
                esaOptions.dictionaryRepository = new DictionaryRepository(dictionary);
            } else {
                esaOptions.dictionaryRepository = new DictionaryRepository();
            }

            String stanfordPosTags = cmd.getOptionValue("stanford-pos");
            boolean stanfordLemmasRequired = nonEmpty(stanfordPosTags);
            boolean stanfordLemmasFound = false;

            DocumentPreprocessorFactory preprocessorFactory = new DocumentPreprocessorFactory();
            ArrayList<DocumentPreprocessor> preprocessors = new ArrayList<>();
            String[] preprocessorArguments = cmd.getOptionValues("preprocessor");
            for(String preprocessorArgument: preprocessorArguments) {
                if ("lemma".equals(preprocessorArgument)) {
                    stanfordLemmasFound = true;
                    preprocessorFactory.setStanfordPosTags(stanfordPosTags);
                }
               preprocessors.add(preprocessorFactory.getPreprocessor(preprocessorArgument));
            }
            if (stanfordLemmasRequired && !stanfordLemmasFound) {
                throw new IllegalArgumentException("The --stanford-pos option requires the --preprocessor stanford-lemma option to be set.");
            }
            esaOptions.preprocessor = new ChainedPreprocessor(preprocessors);;


            AnalyzerFactory analyzerFactory = new AnalyzerFactory(esaOptions.documentType);
            esaOptions.analyzer = analyzerFactory.getAnalyzer();


            //Load indexer options from command line and ESA options
            WikiIndexerOptions indexerOptions = new WikiIndexerOptions();
            loadIndexerOptions(indexerOptions, esaOptions, cmd);
            indexerOptions.preprocessor = esaOptions.preprocessor;
            indexerOptions.analyzer = esaOptions.analyzer;
            indexerOptions.indexDirectory = FSDirectory.open(esaOptions.indexPath);

            String server = cmd.getOptionValue("server");
            String debug = cmd.getOptionValue("d");
            String index = cmd.getOptionValue("i");
            esaOptions.indexFile = index;

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
                TextVectorizer textVectorizer = new Vectorizer(esaOptions);
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
                TextVectorizer textVectorizer = new Vectorizer(esaOptions);
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
                TokenStream ts = esaOptions.analyzer.tokenStream("text", sourceText);
                try (ts) {
                    CharTermAttribute charTermAttribute = ts.addAttribute(CharTermAttribute.class);
                    TypeAttribute typeAttribute = ts.addAttribute(TypeAttribute.class);
                    ts.reset();
                    while (ts.incrementToken()) {
                        System.out.println(typeAttribute.type() + ": " + charTermAttribute);
                    }
                    ts.end();
                }
            }

            else if(hasLength(weightArgs, 2)) {
                String documentId = weightArgs[0];
                String documentText = weightArgs[1];
                Directory dir = FSDirectory.open(esaOptions.indexPath);
                IndexReader docReader = DirectoryReader.open(dir);
                IndexSearcher docSearcher = new IndexSearcher(docReader);

                Term idTerm = new Term(DreamIndexer.ID_FIELD, documentId);
                WeighedDocumentQueryBuilder builder = new WeighedDocumentQueryBuilder(esaOptions.analyzer, docSearcher);
                System.out.println("Weighted Query: " + builder.weight(idTerm, documentText));
            }

            //Relevance of term to document
            else if(hasLength(relevanceArgs, 2)) {
                String termString = relevanceArgs[0];
                String documentId = relevanceArgs[1];
                Directory dir = FSDirectory.open(esaOptions.indexPath);
                IndexReader docReader = DirectoryReader.open(dir);
                IndexSearcher docSearcher = new IndexSearcher(docReader);

                //Must include the term
                Term term = new Term("text", termString);
                Term idTerm = new Term("id", documentId);
                DocumentTermRelevance relevance = new DocumentTermRelevance(idTerm, docSearcher);
                System.out.println("Relevance: " + relevance.score(term));
            }

            //Indexing
            else if(nonEmpty(index)) {
                System.out.println("Indexing " + index + "...");
                indexFile(esaOptions, indexerOptions);
            } else if(nonEmpty(server)) {
                Directory dir = FSDirectory.open(Paths.get("./index/" + "dream_termdoc"));
                IndexReader docReader = DirectoryReader.open(dir);
                IndexSearcher docSearcher = new IndexSearcher(docReader);
                WeighedDocumentQueryBuilder builder = new WeighedDocumentQueryBuilder(esaOptions.analyzer, docSearcher);
                TextVectorizer textVectorizer = new Vectorizer(esaOptions);
                int port = 1994;
                try {
                    port = Integer.parseInt(server);
                } catch (NumberFormatException e) {

                }

                //Connect to MySQL
                Map<String, String> env = System.getenv();
                if (!env.containsKey(ODBC_ENVIRONMENT_VARIABLE)) {
                    throw new SQLException("The ODBC connection string was empty: set the " + ODBC_ENVIRONMENT_VARIABLE + " and try again.");
                }
                Connection con = DriverManager.getConnection("jdbc:" + env.get(ODBC_ENVIRONMENT_VARIABLE));
                VectorRepository repository = new VectorRepository(con);

                Gson gson = new Gson();
                Javalin app = Javalin.create().start(port);
                app.post("/vectorize", ctx -> {
                    DocumentVectorizationRequestBody requestBody = gson.fromJson(ctx.body(), DocumentVectorizationRequestBody.class);

                    if (!nonEmpty(requestBody.documentText) || !nonEmpty(requestBody.documentId)) {
                        ctx.res.sendError(400, "Invalid request: documentText and documentId are required fields.");
                    } else {
                        Term idTerm = new Term(DreamIndexer.ID_FIELD, requestBody.documentId);
                        String weightedQuery = builder.weight(idTerm, requestBody.documentText);
                        ConceptVector vector = textVectorizer.vectorize(weightedQuery);
                        DocumentVector documentVector = new DocumentVector(requestBody.documentId);
                        Map<String, Float> conceptWeights = vector.getConceptWeights();
                        for(String concept: conceptWeights.keySet()) {
                            documentVector.addConceptWeight(new ConceptWeight(concept, conceptWeights.get(concept)));
                        }

                        repository.saveDocumentVector(documentVector);
                        ctx.status(200);
                        System.out.println("Processed dream: " + weightedQuery.substring(0, 16) + "...");
                    }
                });

                //Gets top related documents
                app.get("/related", ctx -> {
                    try {
                        String documentId = ctx.queryParam("documentId");
                        String limitParam = ctx.queryParam("limit");
                        if (documentId == null || limitParam == null) {
                            throw new Exception("Invalid request: document and limit are required fields.");
                        }
                        int relatedLimit = Integer.parseInt(limitParam);
                        ctx.json(repository.getRelatedDocuments(documentId, relatedLimit));
                        System.out.println("Related dream: " + documentId);
                    } catch (Exception e) {
                        System.out.println("Failed to relate dream: " + e.getMessage() + ": " + Arrays.toString(e.getStackTrace()));
                        ctx.status(400);
                    }
                });

                //Scores two documents relatedness via their IDs
                app.get("/quick-score", ctx -> {
                    String documentId1 = ctx.queryParam("documentId1");
                    String documentId2 = ctx.queryParam("documentId2");
                    ctx.json(repository.scoreDocuments(documentId1, documentId2));
                });

                //Scores two documents relatedness via their texts, supporting all options
                app.post("/similarity", ctx -> {
                    DocumentSimilarityRequestBody requestBody = gson.fromJson(ctx.body(), DocumentSimilarityRequestBody.class);
                    DocumentSimilarityScorer scorer = new DocumentSimilarityScorer();
                    ctx.json(scorer.score(requestBody));
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

    private static void loadIndexerOptions(WikiIndexerOptions indexerOptions, EsaOptions esaOptions, CommandLine cmd) {
        //Indexer Options
        String minimumTermCount = cmd.getOptionValue("min-terms");
        if (nonEmpty(minimumTermCount)) {
            indexerOptions.minimumTermCount = Integer.parseInt(minimumTermCount);
        }

        String maximumTermCount = cmd.getOptionValue("max-terms");
        if (nonEmpty(maximumTermCount)) {
            indexerOptions.maximumTermCount = Integer.parseInt(maximumTermCount);
        }

        String threadCount = cmd.getOptionValue("threads");
        if (nonEmpty(threadCount)) {
            indexerOptions.threadCount = Integer.parseInt(threadCount);
        }

        String batchSize = cmd.getOptionValue("batch-size");
        if (nonEmpty(batchSize)) {
            indexerOptions.batchSize = Integer.parseInt(batchSize);
        }

        String maximumDocumentCount = cmd.getOptionValue("max-docs");
        if (nonEmpty(maximumDocumentCount)) {
            indexerOptions.maximumDocumentCount = Integer.parseInt(maximumDocumentCount);
        }

        //Indexer Options (Wiki-specific)
        String minimumIncomingLinks = cmd.getOptionValue("min-incoming-links");
        if (nonEmpty(minimumIncomingLinks)) {
            indexerOptions.minimumIncomingLinks = Integer.parseInt(minimumIncomingLinks);
        }

        String minimumOutgoingLinks = cmd.getOptionValue("min-outgoing-links");
        if (nonEmpty(minimumOutgoingLinks)) {
            indexerOptions.minimumOutgoingLinks = Integer.parseInt(minimumOutgoingLinks);
        }

        String[] titleExclusionRegExList = cmd.getOptionValues("title-exclusion-regex");
        if (hasLength(titleExclusionRegExList, 1)) {
            indexerOptions.titleExclusionRegExList.addAll(Arrays.asList(titleExclusionRegExList));
        }

        String[] titleExclusionList = cmd.getOptionValues("title-exclusion");
        if (hasLength(titleExclusionList, 1)) {
            indexerOptions.titleExclusionList.addAll(Arrays.asList(titleExclusionList));
        }
    }

    public static void indexFile(EsaOptions options, WikiIndexerOptions wikiIndexerOptions) {
        IndexerFactory indexerFactory = new IndexerFactory();
        Indexer indexer = indexerFactory.getIndexer(options.documentType, wikiIndexerOptions);
        try{
            indexer.index(new File(options.indexFile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
