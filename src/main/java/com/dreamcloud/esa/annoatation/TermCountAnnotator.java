package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlWritingHandler;
import com.dreamcloud.esa.tools.BZipFileReader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.text.NumberFormat;
import java.util.Map;

public class TermCountAnnotator extends XmlWritingHandler {
    TermCountAnnotatorOptions options;
    protected final SAXParserFactory saxFactory;
    protected int numStripped = 0;

    public TermCountAnnotator(TermCountAnnotatorOptions options) {
        this.options = options;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void annotate(File inputFile, File outputFile) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
        this.open(outputFile);

        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        //Begin the XML document
        this.writeDocumentBegin("docs");

        saxParser.parse(is, this);
        reader.close();

        //End document
        this.writeDocumentEnd();

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + this.getDocsRead());
        System.out.println("Articles Stripped:\t" + numStripped);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Strip Rate:\t" + format.format(((double) numStripped) / ((double) this.getDocsRead())));
        System.out.println("----------------------------------------");
    }

    public void handleDocument(Map<String, String> xmlFields) {
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

        if (this.getDocsRead() % 1000 == 0) {
            System.out.println("counted \t[" + numStripped + " | " + this.getDocsRead() + "]");
        }
    }

    public void writeDocument(Map<String, String> xmlFields, int termCount) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        for(Map.Entry<String, String> xmlField: xmlFields.entrySet()) {
            this.writeElement(xmlField.getKey(), xmlField.getValue());
        }
        this.writeElement("terms", String.valueOf(termCount));
        this.writeEndElement();
    }
}
