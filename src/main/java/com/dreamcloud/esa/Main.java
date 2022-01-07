package com.dreamcloud.esa;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.dreamcloud.esa.analyzer.*;
import com.dreamcloud.esa.annoatation.*;
import com.dreamcloud.esa.database.TfIdfScoreRepository;
import com.dreamcloud.esa.debug.ArticleFinder;
import com.dreamcloud.esa.debug.DebugArticle;
import com.dreamcloud.esa.documentPreprocessor.ChainedPreprocessor;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessor;
import com.dreamcloud.esa.documentPreprocessor.DocumentPreprocessorFactory;
import com.dreamcloud.esa.documentPreprocessor.NullPreprocessor;
import com.dreamcloud.esa.fs.*;
import com.dreamcloud.esa.indexer.*;
import com.dreamcloud.esa.pruner.PrunerTuner;
import com.dreamcloud.esa.pruner.PrunerTuning;
import com.dreamcloud.esa.server.EsaHttpServer;
import com.dreamcloud.esa.similarity.SimilarityFactory;
import com.dreamcloud.esa.tfidf.CollectionInfo;
import com.dreamcloud.esa.tfidf.ScoreReader;
import com.dreamcloud.esa.tfidf.DocumentScoreCachingReader;
import com.dreamcloud.esa.tfidf.TfIdfWriter;
import com.dreamcloud.esa.tools.*;
import com.dreamcloud.esa.vectorizer.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.commons.cli.*;
import org.xml.sax.SAXException;

//Reading input files
import javax.xml.parsers.ParserConfigurationException;
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

        Option vectorizerOption = new Option(null, "vectorizer", true, "string / The type of vectorizer to use (lucene|sql).");
        vectorizerOption.setRequired(false);
        options.addOption(vectorizerOption);

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

        Option similarityOption = new Option(null, "similarity", true, "string | The similarity algorithm to use (TFIDF,BM25,trueTFIDF,trueBM25.");
        similarityOption.setRequired(false);
        options.addOption(similarityOption);

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

        //Analyzer options
        Option limitOption = new Option(null, "vector-limit", true, "int / The maximum number of entries in each document vector.");
        limitOption.setRequired(false);
        options.addOption(limitOption);

        Option stopWordsOption = new Option(null, "stopwords", true, "stopwords file / A file containing stopwords each on their own line");
        stopWordsOption.setRequired(false);
        options.addOption(stopWordsOption);

        Option rareWordsOption = new Option(null, "rare-words", true, "rare words file / A file containing rare words (stop words) each on their own line. Do not combine these with stopwords as these are applied after all of the filter options.");
        rareWordsOption.setRequired(false);
        options.addOption(rareWordsOption);

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

        Option minWordLengthOption = new Option(null, "min-word-length", true, "int / The minimum number of characters for a word to be indexed");
        minWordLengthOption.setRequired(false);
        options.addOption(minWordLengthOption);

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
        Option spearmanOption = new Option(null, "spearman", true, "correlation file / Calculates Spearman correlations to get the p-value of the tool");
        spearmanOption.setRequired(false);
        options.addOption(spearmanOption);

        //Pearson correlations to get tool p-value
        Option pearsonOption = new Option(null, "pearson", true, "correlation file [document file] / Calculates Pearson correlations to get the p-value of the tool");
        pearsonOption.setRequired(false);
        options.addOption(pearsonOption);

        //Pearson correlations to get tool p-value
        Option tuneOption = new Option(null, "tune", true, "int vectorLimitStart int vectorLimitEnd int step / Finds ideal vector limits and pruning options for spearman and pearson");
        tuneOption.setRequired(false);
        options.addOption(tuneOption);

        //Debugging
        Option findArticleOption = new Option(null, "find-article", true, "inputFile articleTitle|index / Displays info about an article searched for via title or numeric index");
        findArticleOption.setRequired(false);
        findArticleOption.setArgs(2);
        options.addOption(findArticleOption);

        Option debugOption = new Option(null, "debug", true, "input.txt / Shows the tokens for a text.");
        debugOption.setRequired(false);
        options.addOption(debugOption);

        //Annotating
        Option wikiPreprocessorOption = new Option(null, "preprocess", true, "inputFile outputFile titleMapOutputFile / Wiki preprocessing: template resolution, title normalization, article stripping");
        wikiPreprocessorOption.setRequired(false);
        wikiPreprocessorOption.setArgs(3);
        options.addOption(wikiPreprocessorOption);

        Option linkCountOption = new Option(null, "count-links-and-terms", true, "wikiInputFile titleMapFile outputFile / Creates an annotated XML file with link counts.");
        linkCountOption.setRequired(false);
        linkCountOption.setArgs(3);
        options.addOption(linkCountOption);

        Option repeatContentOption = new Option(null, "repeat-content", true, "inputFile outputFile / Repeats titles and links to weight them more highly.");
        repeatContentOption.setRequired(false);
        repeatContentOption.setArgs(2);
        options.addOption(repeatContentOption);

        Option categoryInfoOption = new Option(null, "category-info", true, "inputFile / Displays category information about the processed dump file.");
        categoryInfoOption.setRequired(false);
        options.addOption(categoryInfoOption);

        Option repeatTitleOption = new Option(null, "repeat-title", true, "int | The number of times to repeat titles.");
        repeatTitleOption.setRequired(false);
        options.addOption(repeatTitleOption);

        Option repeatLinkOption = new Option(null, "repeat-link", true, "int | The number of times to repeat links.");
        repeatLinkOption.setRequired(false);
        options.addOption(repeatLinkOption);

        Option writeRareWordsOption = new Option(null, "write-rare-words", true, "inputFile outputFile rareWordCount / Creates a text file of rare words to later be used for stopwords.");
        writeRareWordsOption.setRequired(false);
        writeRareWordsOption.setArgs(3);
        options.addOption(writeRareWordsOption);

        Option articleStatsOption = new Option(null, "article-stats", true, "inputFile outputFile / Gets stats about the annotated articles.");
        articleStatsOption.setRequired(false);
        options.addOption(articleStatsOption);

        //Data source (DB, Lucene, custom FS)
        Option sourceOption = new Option(null, "source", true, "[db|fs|lucene] / The data source for analysis.");
        sourceOption.setRequired(true);
        options.addOption(sourceOption);

        //Indexing
        Option indexOption = new Option(null, "index", true, "input file / Indexes a corpus of documents.");
        indexOption.setRequired(false);
        options.addOption(indexOption);

        //Index path
        Option indexPathOption = new Option(null, "index-path", true, "input directory / The path to the input directory (defaults to ./index/$doctype)");
        indexPathOption.setRequired(false);
        options.addOption(indexPathOption);

        //Prune index option
        Option pruneOption = new Option(null, "prune", false, "Creates an inverse index mapping pruned to include only relevant concepts");
        pruneOption.setRequired(false);
        options.addOption(pruneOption);

        Option pruneWindowSizeOption = new Option(null, "prune-window-size", true, "int [100] / The size of the concept window used for pruning.");
        pruneWindowSizeOption.setRequired(false);
        options.addOption(pruneWindowSizeOption);

        Option pruneDropOffOption = new Option(null, "prune-dropoff", true, "double [0.05] / The dropoff used to limit concepts during pruning.");
        pruneDropOffOption.setRequired(false);
        options.addOption(pruneDropOffOption);

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
            String[] wikiPreprocessorArgs = cmd.getOptionValues("preprocess");
            String[] findArticleArgs = cmd.getOptionValues("find-article");
            String[] countLinkArgs = cmd.getOptionValues("count-links-and-terms");
            String[] repeatContentArgs = cmd.getOptionValues("repeat-content");
            String[] writeRareWordArgs = cmd.getOptionValues("write-rare-words");
            String[] pearsonArgs = cmd.getOptionValues("pearson");
            String[] sourceArgs = cmd.getOptionValues("source");
            String docType = cmd.getOptionValue("doctype");
            String vectorizerType = cmd.getOptionValue("vectorizer");
            String similarityAlgorithm = cmd.getOptionValue("similarity");
            String stopWords = cmd.getOptionValue("stopwords");
            String rareWords = cmd.getOptionValue("rare-words");
            String dictionary = cmd.getOptionValue("dictionary");
            String indexPath = cmd.getOptionValue("index-path");
            String pruneWindowSize = cmd.getOptionValue("prune-window-size");
            String pruneDropOff = cmd.getOptionValue("prune-dropoff");
            String titleRepeat = cmd.getOptionValue("repeat-title");
            String linkRepeat = cmd.getOptionValue("repeat-link");
            String categoryInfo = cmd.getOptionValue("category-info");

            SourceOptions sourceOptions = new SourceOptions();
            String source = sourceArgs[0];
            if (source.equals("db")) {
                TfIdfScoreRepository repo = new TfIdfScoreRepository();
                sourceOptions.collectionInfo = new CollectionInfo(repo.getDocumentCount(), repo.getDocumentFrequencies());
                sourceOptions.scoreReader = repo;
            } else if(source.equals("fs")) {
                TermIndexReader termIndexReader = new TermIndexReader();
                termIndexReader.open(new File("term-index.dc"));
                TermIndex termIndex = termIndexReader.readIndex();
                DocumentScoreDataReader scoreFileReader = new DocumentScoreFileReader(new File("term-scores.dc"));
                sourceOptions.collectionInfo = new CollectionInfo(termIndex.getDocumentCount(), termIndex.getDocumentFrequencies());
                sourceOptions.scoreReader = new ScoreReader(termIndex, scoreFileReader);
            }
            //sourceOptions.scoreReader = new DocumentScoreCachingReader(sourceOptions.scoreReader);
            LoggingScoreReader scoreReader = new LoggingScoreReader(sourceOptions.scoreReader);
            sourceOptions.scoreReader = scoreReader;

            EsaOptions esaOptions = new EsaOptions();
            esaOptions.sourceOptions = sourceOptions;

            PruneOptions pruneOptions = new PruneOptions();
            if (nonEmpty(pruneWindowSize)) {
                pruneOptions.windowSize = Integer.parseInt(pruneWindowSize);
            }
            if (nonEmpty(pruneDropOff)) {
                pruneOptions.dropOff = Float.parseFloat(pruneDropOff);
            }
            esaOptions.pruneOptions = pruneOptions;

            if (!nonEmpty(docType)) {
                docType = "wiki";
            }
            esaOptions.documentType = DocumentType.valueOfLabel(docType);
            if ( esaOptions.documentType == null) {
                throw new IllegalArgumentException("Document type " + docType + " is not recognized.");
            }
            esaOptions.indexPath = Paths.get(nonEmpty(indexPath) ? indexPath : "./index/" + docType + "_index");

            if (nonEmpty(vectorizerType)) {
                esaOptions.vectorizerType = vectorizerType;
            } else {
                esaOptions.vectorizerType = "lucene";
            }

            if (nonEmpty(similarityAlgorithm)) {
                SimilarityFactory.algorithm = similarityAlgorithm;
            }

            String limit = cmd.getOptionValue("vector-limit");
            int documentLimit = 0;
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

            if (nonEmpty(rareWords)) {
                esaOptions.rareWordRepository = new StopWordRepository(rareWords);
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

            CommandLineVectorizerFactory vectorizerFactory = new CommandLineVectorizerFactory(esaOptions);


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
                TextVectorizer textVectorizer = vectorizerFactory.getVectorizer();
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
                TextVectorizer textVectorizer = vectorizerFactory.getVectorizer();
                ConceptVector vector = textVectorizer.vectorize(sourceText);
                Iterator<Integer> topTenConcepts = vector.topConcepts();
                while (topTenConcepts.hasNext()) {
                    int concept = topTenConcepts.next();
                    System.out.println(concept + ": " + decimalFormat.format(vector.getConceptWeights().get(concept)));
                }
            }

            else if (cmd.hasOption("spearman")) {
                String spearman = cmd.getOptionValue("spearman");
                if ("en-wordsim353".equals(spearman)) {
                    spearman = "./src/data/en-wordsim353.csv";
                }
                TextVectorizer textVectorizer = vectorizerFactory.getVectorizer();
                SemanticSimilarityTool similarityTool = new SemanticSimilarityTool(textVectorizer);
                PValueCalculator calculator = new PValueCalculator(new File(spearman));
                System.out.println("Calculating P-value using Spearman correlation...");
                System.out.println("----------------------------------------");
                System.out.println("p-value:\t" + calculator.getSpearmanCorrelation(similarityTool));
                System.out.println("----------------------------------------");
            }

            else if (hasLength(pearsonArgs, 1)) {
                String pearsonFile = pearsonArgs[0];
                File documentFile = pearsonArgs.length > 1 ? new File(pearsonArgs[1]) : null;
                if ("en-lp50".equals(pearsonFile)) {
                    pearsonFile = "./src/data/en-lp50.csv";
                    documentFile = new File("./src/data/en-lp50-documents.csv");
                }
                TextVectorizer textVectorizer = vectorizerFactory.getVectorizer();
                SemanticSimilarityTool similarityTool = new SemanticSimilarityTool(textVectorizer);
                PValueCalculator calculator = new PValueCalculator(new File(pearsonFile), documentFile);
                System.out.println("Calculating P-value using Pearson correlation...");
                System.out.println("----------------------------------------");
                System.out.println("p-value:\t" + calculator.getPearsonCorrelation(similarityTool));
                System.out.println("----------------------------------------");
            }

            else if (cmd.hasOption("tune")) {
                File spearmanFile = new File("./src/data/en-wordsim353.csv");
                File pearsonFile = new File("./src/data/en-lp50.csv");
                File documentFile = new File("./src/data/en-lp50-documents.csv");

                TextVectorizer textVectorizer = vectorizerFactory.getVectorizer();
                PValueCalculator spearmanCalculator = new PValueCalculator(spearmanFile);
                PValueCalculator pearsonCalculator = new PValueCalculator(pearsonFile, documentFile);

                SemanticSimilarityTool similarityTool = new SemanticSimilarityTool(textVectorizer);
                PrunerTuner tuner = new PrunerTuner(similarityTool);
                System.out.println("Analyzing wordsim-353 to find the ideal vector limit...");
                System.out.println("----------------------------------------");
                PrunerTuning tuning = tuner.tune(pearsonCalculator, pruneOptions, 100, 150, 10, 0.001, 0.25, 0.001);
                System.out.println("tuned p-value:\t" + tuning.getTunedScore());
                System.out.println("tuned window size:\t" + tuning.getTunedWindowSize());
                System.out.println("tuned window dropoff:\t" + tuning.getTunedWindowDropOff());
                System.out.println("----------------------------------------");
            }

            //Debugging

            //Display article text
            else if(hasLength(findArticleArgs, 2)) {
                ArticleFinder af = new ArticleFinder(new File(findArticleArgs[0]));
                System.out.println("Article");
                System.out.println("----------------------------------------");
                DebugArticle article = af.find(findArticleArgs[1]);
                if (article != null) {
                    System.out.println("index:\t" + article.index);
                    System.out.println("title:\t" + article.title);
                    System.out.println("string(" + article.text.length() + ")");
                    System.out.println(article.text);
                } else {
                    System.out.println("article not found");
                }
                System.out.println("----------------------------------------");
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
                docSearcher.setSimilarity(SimilarityFactory.getSimilarity());
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
                docSearcher.setSimilarity(SimilarityFactory.getSimilarity());
                //Must include the term
                Term term = new Term("text", termString);
                Term idTerm = new Term("id", documentId);
                DocumentTermRelevance relevance = new DocumentTermRelevance(idTerm, docSearcher);
                System.out.println("Relevance: " + relevance.score(term));
            }

            else if(hasLength(wikiPreprocessorArgs, 3)) {
                File inputFile = new File(wikiPreprocessorArgs[0]);
                File outputFile = new File(wikiPreprocessorArgs[1]);
                File titleMapOutputFile = new File(wikiPreprocessorArgs[2]);
                WikiPreprocessorOptions wikiPreprocessorOptions = new WikiPreprocessorOptions();
                wikiPreprocessorOptions.titleExclusionRegExList = indexerOptions.titleExclusionRegExList;
                try (WikiPreprocessor wikiPreprocessor = new WikiPreprocessor(wikiPreprocessorOptions);) { wikiPreprocessor.preprocess(inputFile, outputFile, titleMapOutputFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            else if (hasLength(countLinkArgs, 3)) {
                File strippedFile = new File(countLinkArgs[0]);
                File titleMapFile = new File(countLinkArgs[1]);
                File outputFile = new File(countLinkArgs[2]);
                WikiLinkAndTermAnnotatorOptions wikiLinkAnnotatorOptions = new WikiLinkAndTermAnnotatorOptions();
                wikiLinkAnnotatorOptions.minimumIncomingLinks = indexerOptions.minimumIncomingLinks;
                wikiLinkAnnotatorOptions.minimumOutgoingLinks = indexerOptions.minimumOutgoingLinks;
                wikiLinkAnnotatorOptions.minimumTerms = indexerOptions.minimumTermCount;
                wikiLinkAnnotatorOptions.analyzer = indexerOptions.analyzerFactory.getAnalyzer();
                try(WikiLinkAndTermAnnotator annotator = new WikiLinkAndTermAnnotator(wikiLinkAnnotatorOptions)) {
                    annotator.annotate(strippedFile, titleMapFile, outputFile);
                }
            }

            else if (hasLength(repeatContentArgs, 2)) {
                File inputFile = new File(repeatContentArgs[0]);
                File outputFile = new File(repeatContentArgs[1]);
                WikiContentRepeatOptions repeatOptions = new WikiContentRepeatOptions();
                if (nonEmpty(titleRepeat)) {
                    repeatOptions.titleRepeat = Integer.parseInt(titleRepeat);
                }
                if (nonEmpty(linkRepeat)) {
                    repeatOptions.linkRepeat = Integer.parseInt(linkRepeat);
                }
                try(WikiContentRepeater repeater = new WikiContentRepeater(repeatOptions)) {
                    repeater.repeatContent(inputFile, outputFile);
                }
            }

            else if (hasLength(writeRareWordArgs, 3)) {
                File inputFile = new File(writeRareWordArgs[0]);
                File outputFile = new File(writeRareWordArgs[1]);
                int rareWordThreshold = Integer.parseInt(writeRareWordArgs[2]);
                try(RareWordDictionary termCountMapper = new RareWordDictionary(analyzerFactory.getAnalyzer(), rareWordThreshold)) {
                    termCountMapper.mapToXml(inputFile, outputFile);
                }
            }

            else if (cmd.hasOption("article-stats")) {
                File inputFile = new File(cmd.getOptionValue("article-stats"));
                int rareTerms = 0;
                if (esaOptions.rareWordRepository != null) {
                    rareTerms = esaOptions.rareWordRepository.getStopWords().size();
                }
                try(ArticleStatsReader articleStatsReader = new ArticleStatsReader(analyzerFactory.getAnalyzer(), rareTerms)) {
                    articleStatsReader.readArticles(inputFile);
                }
            }

            else if(nonEmpty(categoryInfo)) {
                File inputFile = new File(categoryInfo);
                try(CategoryAnalyzer categoryAnalyzer = new CategoryAnalyzer()) {
                    categoryAnalyzer.analyze(inputFile, null);
                }
            }

            //Indexing
            else if(nonEmpty(index)) {
                System.out.println("Indexing " + index + "...");
                indexFile(esaOptions, indexerOptions);
            } else if(cmd.hasOption("prune")) {
              IndexPruner pruner = new IndexPruner(pruneOptions);
              pruner.prune(new TfIdfScoreRepository());
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
            System.out.println("Read " + scoreReader.getTermsRead() + " terms @ " + decimalFormat.format(scoreReader.getTermsReadPerSecond()) + " terms/s.");
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
    }

    public static void indexFile(EsaOptions options, WikiIndexerOptions wikiIndexerOptions) throws IOException, ParserConfigurationException, SAXException {
        //Get document frequencies
        RareWordDictionary rareWordDictionary = new RareWordDictionary(options.analyzer, 0);
        rareWordDictionary.parse(new File(options.indexFile));
        CollectionInfo collectionInfo = new CollectionInfo(rareWordDictionary.getDocsRead(), rareWordDictionary.getDocumentFrequencies());

        TfIdfWriter writer = new TfIdfWriter(new File(options.indexFile), collectionInfo, wikiIndexerOptions);
        writer.index();
        //writer.index(new File(options.indexFile));
        /*IndexerFactory indexerFactory = new IndexerFactory();
        Indexer indexer = indexerFactory.getIndexer(options.documentType, wikiIndexerOptions);
        try{
            indexer.index(new File(options.indexFile));
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}
