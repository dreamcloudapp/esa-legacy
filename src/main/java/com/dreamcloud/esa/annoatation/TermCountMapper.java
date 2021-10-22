package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.tools.BZipFileReader;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
import java.util.HashSet;
import java.util.Set;

public class TermCountMapper extends DefaultHandler {
    protected final SAXParserFactory saxFactory;
    protected boolean inDoc;
    protected boolean inDocText;
    protected StringBuilder content = new StringBuilder();
    protected int articlesRead = 0;
    protected int termsRead = 0;
    protected MutableObjectIntMap<String> termCounts = ObjectIntMaps.mutable.empty();
    protected MutableObjectIntMap<String> uniqueTermCounts = ObjectIntMaps.mutable.empty();
    protected XMLStreamWriter xmlWriter;
    Analyzer analyzer;

    public TermCountMapper(Analyzer analyzer) {
        this.analyzer = analyzer;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void mapToXml(File inputFile, File outputFile) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
        //Build the map
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, this);
        reader.close();

        //Write the map
        OutputStream outputStream = new FileOutputStream(outputFile);
        outputStream = new BufferedOutputStream(outputStream, 4096 * 4);
        //outputStream = new BZip2CompressorOutputStream(outputStream);
        this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");

        this.writeDocumentBegin();

        this.writeDocumentEnd();

        xmlWriter.close();
        outputStream.close();

        System.out.print("Term Statistics:");
        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + articlesRead);
        System.out.println("Terms Read:\t" + termsRead);
        System.out.println("Terms per Article:\t" + termsRead / articlesRead);
        System.out.println("Unique Terms:\t" + termCounts.size());
        System.out.println("----------------------------------------");
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("doc".equals(localName)) {
            inDoc = true;
        } else if (inDoc && "text".equals(localName)) {
            inDocText = true;
            content = new StringBuilder();
        }
    }

    public void endElement(String uri, String localName, String qName) {
         if (inDoc && inDocText && "text".equals(localName)) {
             articlesRead++;
            inDocText = false;
            String text = content.toString();
             TokenStream tokens = analyzer.tokenStream("text", text);
             CharTermAttribute termAttribute = tokens.addAttribute(CharTermAttribute.class);
             Set<String> uniqueTerms = new HashSet<>();
             try {
                 tokens.reset();
                 while(tokens.incrementToken()) {
                     this.termCounts.addToValue(termAttribute.toString(), 1);
                     termsRead++;
                     uniqueTerms.add(termAttribute.toString());
                 }
                 tokens.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }

             for (String term: uniqueTerms) {
                 this.uniqueTermCounts.addToValue(term, 1);
             }
        } else if (inDoc && "doc".equals(localName)) {
            inDoc = false;
        }
    }

    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }

    protected void writeDocumentBegin() throws XMLStreamException {
        this.xmlWriter.writeStartDocument();
        xmlWriter.writeStartElement("docs");
    }

    protected void writeDocumentEnd() throws XMLStreamException {
        xmlWriter.writeEndElement();
        xmlWriter.writeEndDocument();
    }

    protected void writeDocument(String title, String redirect) throws XMLStreamException {
        xmlWriter.writeStartElement("doc");

        xmlWriter.writeStartElement("title");
        xmlWriter.writeCharacters(title);
        xmlWriter.writeEndElement();

        xmlWriter.writeStartElement("redirect");
        xmlWriter.writeCharacters(redirect);
        xmlWriter.writeEndElement();

        xmlWriter.writeEndElement();
    }
}
