package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.tools.BZipFileReader;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
import java.text.NumberFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TermCountAnnotator extends DefaultHandler {
    TermCountAnnotatorOptions options;

    protected final SAXParserFactory saxFactory;
    protected boolean inDoc;
    protected String xmlTag;
    protected Map<String, String> xmlFields;
    protected StringBuilder content = new StringBuilder();
    protected String title;
    protected int numRead = 0;
    protected int numStripped = 0;
    protected XMLStreamWriter xmlWriter;

    public TermCountAnnotator(TermCountAnnotatorOptions options) {
        this.options = options;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void annotate(File inputFile, File outputFile) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
//Prepare to write bzipped XML
        OutputStream outputStream = new FileOutputStream(outputFile);
        outputStream = new BufferedOutputStream(outputStream, 4096 * 4);
        outputStream = new BZip2CompressorOutputStream(outputStream);
        this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");

        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        //Begin the XML document
        this.writeDocumentBegin();

        saxParser.parse(is, this);
        reader.close();

        //End document
        this.writeDocumentEnd();
        xmlWriter.close();
        outputStream.close();

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
        if ("doc".equals(localName)) {
            inDoc = true;
            xmlFields = new ConcurrentHashMap<>();
        } else if (inDoc) {
            content = new StringBuilder();
            xmlTag = localName;
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (inDoc) {
            if ("doc".equals(localName)) {
                inDoc = false;
                numRead++;

                try {
                    //Get the text field
                    String text = xmlFields.getOrDefault("text", null);
                    if (text == null) {
                        numStripped++;
                        return;
                    }

                    //Analyze the text!
                    TokenStream tokens = options.analyzer.tokenStream("text", text);
                    CharTermAttribute termAttribute = tokens.addAttribute(CharTermAttribute.class);
                    tokens.reset();
                    int tokenCount = 0;
                    while(tokens.incrementToken()) {
                        tokenCount++;
                    }
                    tokens.close();

                    if (tokenCount < options.minimumTerms || (options.maximumTerms > 0 && tokenCount > options.maximumTerms)) {
                        numStripped++;
                        return;
                    }
                    this.writeDocument(xmlFields, tokenCount);
                } catch (XMLStreamException | IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                if (numRead % 1000 == 0) {
                    System.out.println("counted \t[" + numStripped + " | " + numRead + "]");
                }
            } else {
                xmlFields.put(xmlTag, content.toString());
            }
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

    public void writeDocument(Map<String, String> xmlFields, int termCount) throws XMLStreamException {
        xmlWriter.writeStartElement("doc");
        for(Map.Entry<String, String> xmlField: xmlFields.entrySet()) {
            xmlWriter.writeStartElement(xmlField.getKey());
            xmlWriter.writeCharacters(xmlField.getValue());
            xmlWriter.writeEndElement();
        }

        xmlWriter.writeStartElement("terms");
        xmlWriter.writeCharacters(String.valueOf(termCount));
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();
    }
}
