package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.tools.BZipFileReader;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Takes a stripped dump file and a mapping of redirect titles
 * and adds the following information:
 * <docs>
 *     <doc>
 *         <title>Cat</title>
 *         <text>Cats are small, furry, and cute mammals.</text>
 *         <incomingLinks>24</incomingLinks>
 *         <outgoingLinks>24</incomingLinks>
 *     </doc>
 * </docs>
 *
 * This will be saved as an XML file,
 * with the option to exclude things that don't meed the minimum criteria.
 * This results in a smaller file size,
 * but makes the dump less versatile.
 */
public class WikiLinkAnnotator extends DefaultHandler {
    protected AnnotatorOptions options;
    protected Map<String, String> titleMap = new HashMap<>();
    protected Map<String, WikiLinkAnnotation> annotations = new HashMap<>();

    protected final SAXParserFactory saxFactory;
    protected boolean inDoc;
    protected boolean inDocTitle;
    protected boolean inDocText;
    protected StringBuilder content = new StringBuilder();
    protected String title;
    protected int numRead = 0;
    protected int numStripped = 0;

    public WikiLinkAnnotator(AnnotatorOptions options) {
        this.options = options;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    protected void reset() {
        annotations.clear();
        titleMap.clear();
    }

    public void annotate(File strippedFile, File titleMapFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        reset();
        buildTitleMap(titleMapFile);
        System.out.println("Title Map: " + titleMap.size());
        System.out.println("---------------------------------------");
        /*for(String title: titleMap.keySet()) {
            String redirect = titleMap.get(title);
            System.out.println(title + "\t->\t" + redirect);
        }*/
        System.out.println("---------------------------------------");
        analyzeDocuments(strippedFile);
        System.out.println("Annotations: " + annotations.size());
        float totalIncomingLinks = 0;
        float totalOutgoingLinks = 0;
        for (WikiLinkAnnotation annotation: annotations.values()) {
            totalIncomingLinks += annotation.incomingLinks;
            totalOutgoingLinks += annotation.outgoingLinks;
        }
        System.out.println("Link Stats: " + titleMap.size());
        System.out.println("---------------------------------------");
        System.out.println("Average Incoming Links: " + (totalIncomingLinks / annotations.size()));
        System.out.println("Average Outgoing Links: " + (totalOutgoingLinks / annotations.size()));
        System.out.println("---------------------------------------");

        writeAnnotatedXml(strippedFile, outputFile);
    }

    protected void buildTitleMap(File titleMapFile) throws IOException, ParserConfigurationException, SAXException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(titleMapFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, new WikiTitleMapHandler(titleMap));
        reader.close();
    }

    protected void analyzeDocuments(File strippedFile) throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(strippedFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        /*AnalyzerOptions analyzerOptions = new AnalyzerOptions();
        analyzerOptions.stopWordsRepository = options.stopWordRepository;
        EsaAnalyzer analyzer = new WikiLinkAnalyzer(analyzerOptions);*/
        saxParser.parse(is, new WikiLinkHandler(titleMap, annotations));
        reader.close();
    }

    protected void writeAnnotatedXml(File strippedFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        OutputStream outputStream = new FileOutputStream(outputFile);
        outputStream = new BufferedOutputStream(outputStream, 4096 * 4);
        outputStream = new BZip2CompressorOutputStream(outputStream);
        XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");

        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(strippedFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        WikiLinkXmlWritingHandler handler = new WikiLinkXmlWritingHandler(titleMap, annotations, options, xmlWriter);
        handler.writeDocumentBegin();
        saxParser.parse(is, handler);
        reader.close();
        handler.writeDocumentEnd();
        xmlWriter.close();

        System.out.println("Link Annotation Stats:");
        System.out.println("---------------------------------------");
        System.out.println("Articles Read:\t" + handler.numRead);
        System.out.println("Articles Skipped:\t" + handler.numSkipped);
        System.out.println("Articles Written:\t" + (handler.numRead - handler.numSkipped));
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Skip Rate:\t" + handler.numSkipped / handler.numRead);
        System.out.println("---------------------------------------");
    }
}
