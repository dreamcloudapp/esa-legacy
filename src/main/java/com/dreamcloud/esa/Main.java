package com.dreamcloud.esa;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import com.dreamcloud.esa.pruner.PrunerTuner;
import com.dreamcloud.esa.pruner.PrunerTuning;
import com.dreamcloud.esa.tools.*;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) throws IOException, ParseException {
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(3);
        format.setMinimumFractionDigits(3);
        Options options = new Options();

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

        //Spearman correlations to get tool p-value
        Option spearmanOption = new Option(null, "spearman", true, "correlation file / Calculates Spearman correlations to get the p-value of the tool");
        spearmanOption.setRequired(false);
        options.addOption(spearmanOption);

        //Pearson correlations to get tool p-value
        Option pearsonOption = new Option(null, "pearson", true, "correlation file [document file] / Calculates Pearson correlations to get the p-value of the tool");
        pearsonOption.setRequired(false);
        options.addOption(pearsonOption);

        //Pearson correlations to get tool p-value
        Option tuneOption = new Option(null, "tune", false, "Finds ideal pruning options for spearman and pearson");
        tuneOption.setRequired(false);
        options.addOption(tuneOption);

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
            String[] pearsonArgs = cmd.getOptionValues("pearson");

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
                System.out.println("Vector relatedness: " + format.format(similarityTool.findSemanticSimilarity(sourceText, compareText))
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
                for (Integer documentId: vector.getSortedDocumentIds()) {
                    System.out.println(documentId + ": " + format.format(vector.getScore(documentId)));
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

                SemanticSimilarityTool similarityTool = new SemanticSimilarityTool(textVectorizer, pruneOptions);
                PrunerTuner tuner = new PrunerTuner(similarityTool);
                System.out.println("Analyzing wordsim-353 to find the ideal vector limit...");
                System.out.println("----------------------------------------");
                PrunerTuning tuning = tuner.tune(spearmanCalculator, pruneOptions, 1000, 3000, 100, 0.01, 0.05, 0.01);
                System.out.println("tuned p-value:\t" + tuning.getTunedScore());
                System.out.println("tuned window size:\t" + tuning.getTunedWindowSize());
                System.out.println("tuned window dropoff:\t" + tuning.getTunedWindowDropOff());
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
            else {
                formatter.printHelp("wiki-esa", options);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
