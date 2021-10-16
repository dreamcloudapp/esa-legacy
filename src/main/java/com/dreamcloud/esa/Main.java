package com.dreamcloud.esa;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.dreamcloud.esa.analyzer.CommandLineAnalyzerFactory;
import com.dreamcloud.esa.documentPreprocessor.ChainedPreprocessor;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessorFactory;
import com.dreamcloud.esa.documentPreprocessor.NullPreprocessor;
import com.dreamcloud.esa.indexer.DreamIndexer;
import com.dreamcloud.esa.indexer.Indexer;
import com.dreamcloud.esa.indexer.IndexerFactory;
import com.dreamcloud.esa.indexer.WikiIndexerOptions;
import com.dreamcloud.esa.server.EsaHttpServer;
import com.dreamcloud.esa.tools.*;
import com.dreamcloud.esa.vectorizer.ConceptVector;
import com.dreamcloud.esa.vectorizer.TextVectorizer;
import com.dreamcloud.esa.vectorizer.Vectorizer;
import org.apache.commons.math.stat.correlation.SpearmansCorrelation;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.commons.cli.*;

//Reading input files
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
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

        Option weightOption = new Option(null, "weight", true, "docId documentText / Gets the weights of terms within a document");
        weightOption.setArgs(2);
        weightOption.setRequired(false);
        options.addOption(weightOption);

        Option relevanceOption = new Option(null, "relevance", true, "\"term docId\" / Computes the relevance of a term to a document id.");
        relevanceOption.setArgs(2);
        relevanceOption.setRequired(false);
        options.addOption(relevanceOption);

        //Indexing Options
        Option minimumTermCountOption = new Option(null, "min-terms", true, "int / (indexing)\tThe minimum number of terms allowed for a document.");
        minimumTermCountOption.setRequired(false);
        options.addOption(minimumTermCountOption);

        Option maximumTermCountOption = new Option(null, "max-terms", true, "int / (indexing)\tThe maximum number of terms allowed for a document.");
        maximumTermCountOption.setRequired(false);
        options.addOption(maximumTermCountOption);

        Option threadCountOption = new Option(null, "threads", true, "int / (indexing)\tThe number of threads to use.");
        threadCountOption.setRequired(false);
        options.addOption(threadCountOption);

        Option batchSizeOption = new Option(null, "batch-size", true, "int / (indexing)\tThe number of documents to process at once, distributed across the threads.");
        batchSizeOption.setRequired(false);
        options.addOption(batchSizeOption);

        Option maximumDocumentCountOption = new Option(null, "max-docs", true, "int / (indexing)\tThe maximum number of documents we can process before throwing an error (defaults to 512,000).");
        maximumDocumentCountOption.setRequired(false);
        options.addOption(maximumDocumentCountOption);

        //Wiki specific indexing options
        Option minimumIncomingLinksOption = new Option(null, "min-incoming-links", true, "int / (indexing:wiki)\tThe minimum number of incoming links.");
        minimumIncomingLinksOption.setRequired(false);
        options.addOption(minimumIncomingLinksOption);

        Option minimumOutgoingLinksOption = new Option(null, "min-outgoing-links", true, "int / (indexing:wiki)\tThe minimum number of outgoing links.");
        minimumOutgoingLinksOption.setRequired(false);
        options.addOption(minimumOutgoingLinksOption);

        Option titleExclusionRegExListOption = new Option(null, "title-exclusion-regex", true, "string [string2 ...] / (indexing:wiki)\tA list of regexes used to exclude Wiki article titles.");
        titleExclusionRegExListOption.setArgs(Option.UNLIMITED_VALUES);
        titleExclusionRegExListOption.setRequired(false);
        options.addOption(titleExclusionRegExListOption);

        Option titleExclusionListOption = new Option(null, "title-exclusion", true, "string [string2 ...] / (indexing:wiki)\tA list of strings used to exclude Wiki article titles which contain them.");
        titleExclusionListOption.setRequired(false);
        titleExclusionListOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(titleExclusionListOption);

        //Analyzer options
        Option limitOption = new Option(null, "vector-limit", true, "int / The maximum number of entries in each document vector.");
        limitOption.setRequired(false);
        options.addOption(limitOption);

        Option stopWordsOption = new Option(null, "stopwords", true, "stopwords file / A file containing stopwords each on their own line");
        stopWordsOption.setRequired(false);
        options.addOption(stopWordsOption);

        Option dictionaryOption = new Option(null, "dictionary", true, "dictionary file / A file containing a list of allowed words");
        dictionaryOption.setRequired(false);
        options.addOption(dictionaryOption);

        Option filterOption = new Option(null, "filter", true, "string [string2 ...] / List of Lucene analysis filters (stemmer|classic|lower|ascii)");
        filterOption.setRequired(false);
        filterOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(filterOption);

        Option stemmerDepthOption = new Option(null, "stemmer-depth", true, "int / The number of times to apply the stemmer");
        stemmerDepthOption.setRequired(false);
        options.addOption(stemmerDepthOption);

        //Preprocessor options
        Option preprocessorOption = new Option(null, "preprocessor", true, "preprocessor [preprocessor2 ...] / The preprocessors to apply to input and corpus texts.");
        preprocessorOption.setRequired(false);
        preprocessorOption.setArgs(Option.UNLIMITED_VALUES);;
        options.addOption(preprocessorOption);

        Option stanfordPosOption = new Option(null, "stanford-pos", true, "pos [pos2 ...] / The parts of speech to include when using the Stanford Lemma preprocessor");
        stanfordPosOption.setRequired(false);
        stanfordPosOption.setArgs(Option.UNLIMITED_VALUES);
        options.addOption(stanfordPosOption);

        //Spearman correlations to get tool p-value
        Option spearmanOption = new Option(null, "spearman", false, "Calculates Spearman correlations to get the p-value of the tool");
        spearmanOption.setRequired(false);
        options.addOption(spearmanOption);

        //Debugging
        Option debugOption = new Option(null, "debug", true, "input.txt / Shows the tokens for a text.");
        debugOption.setRequired(false);
        options.addOption(debugOption);

        //Indexing
        Option indexOption = new Option(null, "index", true, "input file / Indexes a corpus of documents.");
        indexOption.setRequired(false);
        options.addOption(indexOption);

        //Index path
        Option indexPathOption = new Option(null, "index-path", true, "input directory / The path to the input directory (defaults to ./index/$doctype)");
        indexPathOption.setRequired(false);
        options.addOption(indexPathOption);

        //Server options
        Option serverOption = new Option(null, "server", true, "port / Starts a vectorizing server using the specified port.");
        serverOption.setRequired(false);
        options.addOption(serverOption);

        CommandLineParser parser = new DefaultParser();
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
            String indexPath = cmd.getOptionValue("index-path");

            EsaOptions esaOptions = new EsaOptions();

            if (!nonEmpty(docType)) {
                docType = "wiki";
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
            }

            if (nonEmpty(dictionary)) {
                if (dictionary.equals("en-default")) {
                    dictionary = "./src/data/en-words.txt";
                }
                esaOptions.dictionaryRepository = new DictionaryRepository(dictionary);
            }

            String[] stanfordPosTags = cmd.getOptionValues("stanford-pos");
            boolean stanfordLemmasRequired = hasLength(stanfordPosTags, 1);
            boolean stanfordLemmasFound = false;

            DocumentPreprocessorFactory preprocessorFactory = new DocumentPreprocessorFactory();
            ArrayList<DocumentPreprocessor> preprocessors = new ArrayList<>();
            String[] preprocessorArguments = cmd.getOptionValues("preprocessor");
            if (hasLength(preprocessorArguments, 1)) {
                for(String preprocessorArgument: preprocessorArguments) {
                    if ("stanford-lemma".equals(preprocessorArgument)) {
                        stanfordLemmasFound = true;
                        if (stanfordPosTags != null) {
                            preprocessorFactory.setStanfordPosTags(Arrays.asList(stanfordPosTags));
                        }
                    }
                    preprocessors.add(preprocessorFactory.getPreprocessor(preprocessorArgument));
                }
                if (stanfordLemmasRequired && !stanfordLemmasFound) {
                    throw new IllegalArgumentException("The --stanford-pos option requires the --preprocessor stanford-lemma option to be set.");
                }
                esaOptions.preprocessor = new ChainedPreprocessor(preprocessors);;
            } else {
                esaOptions.preprocessor = new NullPreprocessor();
            }

            CommandLineAnalyzerFactory analyzerFactory = new CommandLineAnalyzerFactory(cmd, esaOptions);
            esaOptions.analyzer = analyzerFactory.getAnalyzer();

            //Load indexer options from command line and ESA options
            WikiIndexerOptions indexerOptions = new WikiIndexerOptions();
            loadIndexerOptions(indexerOptions, esaOptions, cmd);
            indexerOptions.preprocessor = esaOptions.preprocessor;
            indexerOptions.analyzerFactory = analyzerFactory;
            indexerOptions.indexDirectory = FSDirectory.open(esaOptions.indexPath);
            indexerOptions.displayInfo();

            String server = cmd.getOptionValue("server");
            String debug = cmd.getOptionValue("debug");
            String index = cmd.getOptionValue("index");
            esaOptions.indexFile = index;

            esaOptions.displayInfo();

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
                System.out.println("Getting top concepts for '" + sourceDesc + "':");
                TextVectorizer textVectorizer = new Vectorizer(esaOptions);
                ConceptVector vector = textVectorizer.vectorize(sourceText);
                Iterator<String> topTenConcepts = vector.topConcepts();
                for (; topTenConcepts.hasNext(); ) {
                    String concept = topTenConcepts.next();
                    System.out.println(concept + ": " + decimalFormat.format(vector.getConceptWeights().get(concept)));
                }
            }

            else if (cmd.hasOption("spearman")) {
                TextVectorizer textVectorizer = new Vectorizer(esaOptions);
                SemanticSimilarityTool similarityTool = new SemanticSimilarityTool(textVectorizer);
                PValueCalculator calculator = new PValueCalculator();
                System.out.println("Calculating P-value using Spearman correlation...");
                System.out.println("------------------------------");
                System.out.println("p-value:\t" + calculator.getSpearmanCorrelation(similarityTool));
                System.out.println("------------------------------");
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
                ts.close();
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
                EsaHttpServer esaServer = new EsaHttpServer(esaOptions);
                esaServer.start(Integer.parseInt(server));
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
