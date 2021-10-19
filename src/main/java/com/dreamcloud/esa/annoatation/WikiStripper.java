package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.analyzer.WikiLinkAnalyzer;
import com.dreamcloud.esa.analyzer.WikipediaArticle;
import com.dreamcloud.esa.analyzer.WikipediaArticleAnalysis;
import com.dreamcloud.esa.indexer.WikiIndexer;
import com.dreamcloud.esa.indexer.WikiIndexerOptions;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.xml.sax.Attributes;
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
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes a Wikimedia dump file and strips out all of the extra information.
 * It also applies basic title exclusions to reduce the file size.
 *
 * Output structure is:
 * <docs>
 *     <doc>
 *         <title>Cat</title>
 *         <text>Cats are small, furry, and cute mammals.</text>
 *     </doc>
 * </docs>
 */
public class WikiStripper extends DefaultHandler {
    private final SAXParserFactory saxFactory;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();
    private String title;
    private int numRead = 0;
    private int numStripped = 0;
    private XMLStreamWriter xmlWriter;
    ArrayList<Pattern> titleExclusionPatterns;

    public WikiStripper(StripperOptions options) {
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);

        this.titleExclusionPatterns = new ArrayList<>();
        if (options.titleExclusionRegExList != null) {
            for(String titleExclusionRegEx: options.titleExclusionRegExList) {
                this.titleExclusionPatterns.add(Pattern.compile(titleExclusionRegEx));
            }
        }
    }

    protected void reset() {
        numRead = 0;
    }

    public void strip(File inputFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        reset();

        //Prepare to write bzipped XML
        OutputStream outputStream = new FileOutputStream(outputFile);
        outputStream = new BufferedOutputStream(outputStream);
        outputStream = new BZip2CompressorOutputStream(outputStream);
        this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");

        SAXParser saxParser = saxFactory.newSAXParser();
        InputStream inputStream = new FileInputStream(inputFile);
        inputStream = new BufferedInputStream(inputStream);
        inputStream = new BZip2CompressorInputStream(inputStream, true);
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        //Begin the XML document
        this.writeDocumentBegin();

        saxParser.parse(is, this);
        inputStream.close();

        //End document
        this.writeDocumentEnd();

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + numRead);
        System.out.println("Articles Stripped:\t" + numStripped);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Strip Rate:\t" + format.format(((double) numStripped) / ((double) numRead)));
        System.out.println("----------------------------------------");
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("page".equals(localName)) {
            inPage = true;
        } else if (inPage && "title".equals(localName)) {
            inPageTitle = true;
            content = new StringBuilder();
        } else if (inPage && "text".equals(localName)) {
            inPageText = true;
            content = new StringBuilder();
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (inPage && inPageTitle && "title".equals(localName)) {
            inPageTitle = false;
            title = content.toString();
        } else if (inPage && inPageText && "text".equals(localName)) {
            numRead++;

            if (numRead % 1000 == 0) {
                System.out.println("processed article\t[" + numStripped + " / " + numRead + "]");
            }

            inPageText = false;

            //Exclude titles by regex
            for (Pattern pattern: this.titleExclusionPatterns) {
                Matcher matcher = pattern.matcher(title);
                if (matcher.find()) {
                    this.numStripped++;
                    return;
                }
            }

            //Write to the file
            try {
                this.writeDocument(title, content.toString());
            } catch (XMLStreamException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (inPage && "page".equals(localName)) {
            inPage = false;
        }
    }

    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }

    public void writeDocumentBegin() throws XMLStreamException {
        this.xmlWriter.writeStartDocument();
        xmlWriter.writeStartElement("docs");
    }

    public void writeDocumentEnd() throws XMLStreamException {
        xmlWriter.writeEndElement();
        xmlWriter.writeEndDocument();
    }

    public void writeDocument(String title, String text) throws XMLStreamException {
        xmlWriter.writeStartElement("doc");

        xmlWriter.writeStartElement("title");
        xmlWriter.writeCharacters(title);
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("text");
        xmlWriter.writeCharacters(text);
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();
    }
}
