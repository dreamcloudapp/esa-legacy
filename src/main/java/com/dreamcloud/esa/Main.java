package com.dreamcloud.esa;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.dreamcloud.esa.database.ConceptWeight;
import com.dreamcloud.esa.database.DocumentVector;
import com.dreamcloud.esa.database.VectorRepository;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessorFactory;
import com.dreamcloud.esa.server.DocumentSimilarityRequestBody;
import com.dreamcloud.esa.server.DocumentSimilarityScorer;
import com.dreamcloud.esa.server.DocumentVectorizationRequestBody;
import com.dreamcloud.esa.tools.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.*;
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
        return a != null && a.length == length;
    }

    public static void main(String[] args) throws IOException, ParseException {
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

        Option termLookupOption = new Option("term", "term", true, "\"string\" / Get the top concepts for a single term from the inverse mapping.");
        termLookupOption.setRequired(false);
        options.addOption(termLookupOption);

        Option weightOption = new Option("weight", "weight", true, "docId documentText / Gets the weights of terms within a document");
        weightOption.setArgs(2);
        weightOption.setRequired(false);
        options.addOption(weightOption);

        Option relevanceOption = new Option("relevance", "relevance", true, "\"term docId\" / Computes the relevance of a term to a document id.");
        relevanceOption.setArgs(2);
        relevanceOption.setRequired(false);
        options.addOption(relevanceOption);

        Option docTypeOption = new Option("doctype", "doctype", true, "string / The document type (article|dream). Defaults to article.");
        docTypeOption.setRequired(false);
        options.addOption(docTypeOption);

        Option limitOption = new Option("l", "limit", true, "int / The maximum number of concepts to query when comparing texts and finding top concepts.");
        limitOption.setRequired(false);
        options.addOption(limitOption);

        Option stopWordsOption = new Option("stopwords", "stopwords", true, "stopwords file / A file containing stopwords each on their own line");
        stopWordsOption.setRequired(false);

        options.addOption(stopWordsOption);

        Option preprocessorOption = new Option("preprocessor", "preprocessor", true, "preprocessor [preprocessor2 ...] / The preprocessors to apply to input and corpus texts.");
        preprocessorOption.setRequired(false);
        options.addOption(preprocessorOption);

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
        Option indexOption = new Option("i", "index", true, "input file / Indexes a corpus of documents.");
        indexOption.setRequired(false);
        options.addOption(indexOption);

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
            String[] relevanceArgs = cmd.getOptionValues("relevance");
            String[] weightArgs = cmd.getOptionValues("weight");
            String docType = cmd.getOptionValue("doctype");
            String stopWords = cmd.getOptionValue("stopwords");

            if (!nonEmpty(docType)) {
                docType = "article";
            }
            String termDoc = docType + "_" + "termdoc";
            String documentPath = "./index/" + termDoc;

            String limit = cmd.getOptionValue("l");
            int conceptLimit = 1000;
            if (nonEmpty(limit)) {
                try {
                    conceptLimit = Integer.parseInt(limit);
                } catch (NumberFormatException e) {

                }
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

            DocumentPreprocessorFactory preprocessorFactory = new DocumentPreprocessorFactory();
            ArrayList<DocumentPreprocessor> preprocessors = new ArrayList<>();
            String[] preprocessorArguments = cmd.getOptionValues("preprocessor");
            for(String preprocessorArgument: preprocessorArguments) {
               preprocessors.add(preprocessorFactory.getPreprocessor(preprocessorArgument));
            }

            StopWordRepository stopWordRepository;
            if (nonEmpty(stopWords)) {
                if (stopWords.equals("en")) {
                    stopWords = "./src/data/en-stopwords.txt";
                }
                stopWordRepository = new StopWordRepository(stopWords);
            } else {
                stopWordRepository = new StopWordRepository();
            }

            AnalyzerFactory analyzerFactory = new AnalyzerFactory(stopWordRepository);
            VectorizerFactory vectorizerFactory = new VectorizerFactory(analyzerFactory, documentPath, vectorizer, conceptLimit, cohesionValue);


            String server = cmd.getOptionValue("server");
            String debug = cmd.getOptionValue("d");
            String index = cmd.getOptionValue("i");

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
                TextVectorizer textVectorizer = vectorizerFactory.getLemmaVectorizer();
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
                TokenStream ts = analyzerFactory.getVectorizingAnalyzer().tokenStream(WikiIndexer.TEXT_FIELD, sourceText);
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

            else if(hasLength(weightArgs, 2)) {
                String documentId = weightArgs[0];
                String documentText = weightArgs[1];
                Directory dir = FSDirectory.open(Paths.get("./index/" + termDoc));
                IndexReader docReader = DirectoryReader.open(dir);
                IndexSearcher docSearcher = new IndexSearcher(docReader);
                Analyzer analyzer = analyzerFactory.getDreamAnalyzer();

                Term idTerm = new Term(DreamIndexer.ID_FIELD, documentId);
                WeighedDocumentQueryBuilder builder = new WeighedDocumentQueryBuilder(analyzer, docSearcher);
                System.out.println("Weighted Query: " + builder.weight(idTerm, documentText));
            }

            //Relevance of term to document
            else if(hasLength(relevanceArgs, 2)) {
                String termString = relevanceArgs[0];
                String documentId = relevanceArgs[1];
                Directory dir = FSDirectory.open(Paths.get("./index/" + termDoc));
                IndexReader docReader = DirectoryReader.open(dir);
                IndexSearcher docSearcher = new IndexSearcher(docReader);

                //Must include the term
                Term term = new Term(WikiIndexer.TEXT_FIELD, termString);
                Term idTerm = new Term(DreamIndexer.ID_FIELD, documentId);
                DocumentTermRelevance relevance = new DocumentTermRelevance(idTerm, docSearcher);
                System.out.println("Relevance: " + relevance.score(term));
            }

            //Indexing
            else if(nonEmpty(index)) {
                System.out.println("Indexing " + index + "...");
                File wikipediaDumpFile = new File(index);
                indexing(Paths.get("./index/" + termDoc), wikipediaDumpFile, docType);
            } else if(nonEmpty(server)) {
                Directory dir = FSDirectory.open(Paths.get("./index/" + "dream_termdoc"));
                IndexReader docReader = DirectoryReader.open(dir);
                IndexSearcher docSearcher = new IndexSearcher(docReader);
                Analyzer analyzer = AnalyzerFactory.getDreamPostLemmaAnalyzer();
                WeighedDocumentQueryBuilder builder = new WeighedDocumentQueryBuilder(analyzer, docSearcher);
                TextVectorizer textVectorizer = vectorizerFactory.getLemmaVectorizer();
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

    /**
     * Creates a term to concept index from a Wikipedia article dump.
     * @param termDocIndexDirectory The directory where the term to concept index must be created
     * @param wikipediaDumpFile The Wikipedia dump file that must be read to create the index
     * @param stopWords The words that are not used in the semantic analysis
     * @throws IOException
     */
    public static void indexing(Path termDocIndexDirectory, File wikipediaDumpFile, String docType) throws IOException {
        Directory directory = FSDirectory.open(termDocIndexDirectory);
        Indexer indexer;
        if ("article".equals(docType)) {
            System.out.println("indexing wikipedia");
            indexer =  new WikiIndexer(directory);
        } else {
            System.out.println("indexing dreams");
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
