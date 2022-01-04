package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.tools.BZipFileReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArticleStatsReader extends XmlReadingHandler {
    protected final SAXParserFactory saxFactory;
    protected int termsRead = 0;
    protected int articleUniqueTerms = 0;
    protected int rareTerms = 0;
    protected Set<String> allUniqueTerms = new HashSet<>();

    protected int totalIncomingLinks = 0;
    protected int maxIncomingLinks = 0;
    protected String maxIncomingLinksTitle;
    protected int totalOutgoingLinks = 0;
    protected int maxOutgoingLinks = 0;

    Analyzer analyzer;

    public ArticleStatsReader(Analyzer analyzer, int rareTerms) {
        this.analyzer = analyzer;
        this.rareTerms = rareTerms;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void readArticles(File inputFile) throws IOException, ParserConfigurationException, SAXException {
        //Build the map
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, this);
        reader.close();

        System.out.println("Article Stats: ");
        System.out.println("----------------------------------------");
        System.out.println("Articles:\t\t" + this.getDocsRead());
        System.out.println("Total Terms:\t\t" + termsRead + " (stemmed, including rare terms)");
        System.out.println("Avg Terms:\t\t" + ((float) termsRead / this.getDocsRead()) + " (stemmed, including rare terms)");
        System.out.println("Unique Terms:\t\t" + allUniqueTerms.size() + " (stemmed, including rare terms)");
        System.out.println("Avg Unique Terms:\t" + ((float) articleUniqueTerms / this.getDocsRead()) + " (article unrepeated terms)");
        System.out.println("Rare Terms:\t\t" + rareTerms + " (in fewer than 3 articles)");
        System.out.println("Total In-Links:\t\t" + totalIncomingLinks + " (link to valid article)");
        System.out.println("Avg In-Links:\t\t" + ((float) totalIncomingLinks / this.getDocsRead()) + " (link to valid article)");
        System.out.println("Max In-Links:\t\t" + maxIncomingLinks + " (" + maxIncomingLinksTitle + ")");
        System.out.println("Total Out-Links:\t" + totalOutgoingLinks + " (link to valid article)");
        System.out.println("Avg Out-Links:\t\t" + ((float) totalOutgoingLinks / this.getDocsRead()) + " (link to valid article)");
        System.out.println("Max Out-Links:\t\t" + maxOutgoingLinks);
        System.out.println("----------------------------------------");
    }

    @Override
    protected void handleDocument(Map<String, String> xmlFields) throws SAXException {
        String text = xmlFields.get("text");
        int incomingLinks = Integer.parseInt(xmlFields.get("incomingLinks"));
        this.totalIncomingLinks += incomingLinks;
        if (incomingLinks > this.maxIncomingLinks) {
            this.maxIncomingLinks = incomingLinks;
            this.maxIncomingLinksTitle = xmlFields.get("title");
        }

        int outgoingLinks = Integer.parseInt(xmlFields.get("outgoingLinks"));
        this.totalOutgoingLinks += outgoingLinks;
        this.maxOutgoingLinks = Math.max(this.maxOutgoingLinks, outgoingLinks);

        TokenStream tokens = analyzer.tokenStream("text", text);
        CharTermAttribute termAttribute = tokens.addAttribute(CharTermAttribute.class);
        Set<String> uniqueTerms = new HashSet<>();
        try {
            tokens.reset();
            while(tokens.incrementToken()) {
                termsRead++;
                uniqueTerms.add(termAttribute.toString());
                allUniqueTerms.add(termAttribute.toString());
            }
            articleUniqueTerms += uniqueTerms.size();
            tokens.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        this.logMessage("Analyzed article " + this.getDocsRead());
    }
}
