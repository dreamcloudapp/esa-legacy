package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlWritingHandler;
import com.dreamcloud.esa.tools.BZipFileReader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
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
public class WikiLinkAnnotator extends XmlWritingHandler {
    protected WikiLinkAnnotatorOptions options;
    protected Map<String, String> titleMap = new HashMap<>();
    protected Map<String, WikiLinkAnnotation> annotations = new HashMap<>();

    protected final SAXParserFactory saxFactory;
    protected int numStripped = 0;

    public WikiLinkAnnotator(WikiLinkAnnotatorOptions options) {
        this.options = options;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void reset() {
        super.reset();
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
        saxParser.parse(is, new WikiLinkHandler(titleMap, annotations));
        reader.close();
    }

    protected void writeAnnotatedXml(File strippedFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(strippedFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        this.open(outputFile);
        this.writeDocumentBegin("docs");
        saxParser.parse(is, this);
        reader.close();
        this.writeDocumentEnd();

        System.out.println("Link Annotation Stats:");
        System.out.println("---------------------------------------");
        System.out.println("Articles Read:\t" + this.getDocsRead());
        System.out.println("Articles Skipped:\t" + this.numStripped);
        System.out.println("Articles Written:\t" + (this.getDocsRead() - this.numStripped));
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Skip Rate:\t" + format.format((double) this.numStripped / (double) this.getDocsRead()));
        System.out.println("---------------------------------------");
    }

    public void handleDocument(Map<String, String> xmlFields) {
        String title = xmlFields.get("title");
        String text = xmlFields.get("text");
        WikiLinkAnnotation annotation = annotations.getOrDefault(title, null);
        if (annotation != null) {
            if (annotation.incomingLinks < options.minimumIncomingLinks || annotation.outgoingLinks < options.minimumOutgoingLinks) {
                numStripped++;
            } else {
                try {
                    writeDocument(title, text, annotation);
                } catch (XMLStreamException | IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } else {
            numStripped++;
        }

        if (this.getDocsRead() % 1000 == 0) {
            System.out.println("annotated article\t[" + numStripped + " | " + this.getDocsRead() + "]");
        }
    }

    public void writeDocument(String title, String text, WikiLinkAnnotation annotation) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        this.writeElement("title", title);
        this.writeElement("text", text);
        this.writeElement("incomingLinks", String.valueOf(annotation.incomingLinks));
        this.writeElement("outgoingLinks", String.valueOf(annotation.outgoingLinks));
        this.writeEndElement();
    }
}
