package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.EsaOptions;
import com.dreamcloud.esa.analyzer.AnalyzerOptions;
import com.dreamcloud.esa.analyzer.EsaAnalyzer;
import com.dreamcloud.esa.analyzer.WikiLinkAnalyzer;
import com.dreamcloud.esa.indexer.WikiIndexerOptions;
import com.dreamcloud.esa.tools.BZipFileReader;

import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
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
 *         <terms>1492</terms>
 *     </doc>
 * </docs>
 *
 * This will be saved as an XML file,
 * with the option to exclude things that don't meed the minimum criteria.
 * This results in a smaller file size,
 * but makes the dump less versatile.
 */
public class WikiAnnotator extends DefaultHandler {
    protected AnnotatorOptions options;
    protected Map<String, String> titleMap = new HashMap<>();
    protected Map<String, WikiAnnotation> annotations = new HashMap<>();
    protected MutableObjectIntMap<String> incomingLinks = ObjectIntMaps.mutable.empty();
    protected MutableObjectIntMap<String> outgoingLinks = ObjectIntMaps.mutable.empty();

    protected final SAXParserFactory saxFactory;
    protected boolean inDoc;
    protected boolean inDocTitle;
    protected boolean inDocText;
    protected StringBuilder content = new StringBuilder();
    protected String title;
    protected int numRead = 0;
    protected int numStripped = 0;
    protected XMLStreamWriter xmlWriter;

    public WikiAnnotator(AnnotatorOptions options) {
        this.options = options;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    protected void reset() {
        annotations.clear();
        titleMap.clear();
        incomingLinks.clear();
        outgoingLinks.clear();
    }

    public void annotate(File strippedFile, File titleMapFile, File outputFile) throws IOException, ParserConfigurationException, SAXException {
        reset();
        buildTitleMap(titleMapFile);
        System.out.println("Title Map: " + titleMap.size());
        analyzeDocuments(strippedFile);
        System.out.println("Annotations: " + annotations.size());
    }

    private void analyzeDocuments(File strippedFile) throws IOException, SAXException, ParserConfigurationException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(strippedFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        AnalyzerOptions analyzerOptions = new AnalyzerOptions();
        analyzerOptions.stopWordsRepository = options.stopWordRepository;
        EsaAnalyzer analyzer = new WikiLinkAnalyzer(analyzerOptions);
        saxParser.parse(is, new WikiLinkHandler(titleMap, annotations, analyzer));
        reader.close();
    }

    protected void buildTitleMap(File titleMapFile) throws IOException, ParserConfigurationException, SAXException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(titleMapFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, new WikiTitleMapHandler(titleMap));
        reader.close();
    }
}
