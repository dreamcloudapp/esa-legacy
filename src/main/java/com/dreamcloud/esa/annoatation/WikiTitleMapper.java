package com.dreamcloud.esa.annoatation;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//#REDIRECT [[Cat]]
/**
 * Takes a Wikimedia dump file, and generates a mapping of normalized titles.
 * This mapping will be written to an XML file in the following format:
 * <docs>
 *     <doc>
 *         <title>United States of America</title>
 *         <redirect>United States</redirect>
 *     </doc>
 * </docs>
 */
public class WikiTitleMapper extends DefaultHandler {
    protected String redirectPattern = "^#REDIRECT \\[\\[(.+)]]$";
    protected Pattern pattern;
    protected File inputFile;

    protected final SAXParserFactory saxFactory;
    protected boolean inPage;
    protected boolean inPageTitle;
    protected boolean inPageText;
    protected StringBuilder content = new StringBuilder();
    protected String title;
    protected int numRead = 0;
    protected int numRedirects = 0;
    protected XMLStreamWriter xmlWriter;

    public WikiTitleMapper(File inputFile) {
        this.inputFile = inputFile;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
        pattern = Pattern.compile(redirectPattern);
    }

    public void mapToXml(File outputFile) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
        OutputStream outputStream = new FileOutputStream(outputFile);
        outputStream = new BufferedOutputStream(outputStream, 4096 * 4);
        outputStream = new BZip2CompressorOutputStream(outputStream);
        this.xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");

        this.writeDocumentBegin();
        this.parse();
        this.writeDocumentEnd();

        xmlWriter.close();
        outputStream.close();

        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + numRead);
        System.out.println("Articles Redirected:\t" + numRedirects);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Redirection Rate:\t" + format.format(((double) numRedirects) / ((double) numRead)));
        System.out.println("----------------------------------------");
    }

    protected void parse() throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = saxFactory.newSAXParser();
        InputStream inputStream = new FileInputStream(inputFile);
        inputStream = new BufferedInputStream(inputStream);
        inputStream = new BZip2CompressorInputStream(inputStream, true);
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, this);
        inputStream.close();
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
            title = content.toString().replaceAll("[+\\-&|!(){}\\[\\]^\"~*?:;,/\\\\]+", " ").toLowerCase();
        } else if (inPage && inPageText && "text".equals(localName)) {
            numRead++;

            if (numRead % 1000 == 0) {
                System.out.println("processed article\t[" + numRedirects + " / " + numRead + "]");
            }

            inPageText = false;

            String articleText = content.toString();
            //Check to see if it's a redirection article
            Matcher matcher = pattern.matcher(articleText);
            if (matcher.matches()) {
                numRedirects++;
                //Write XML
                try {
                    this.writeDocument(title, matcher.group(1).replaceAll("[+\\-&|!(){}\\[\\]^\"~*?:;,/\\\\]+", " ").toLowerCase());
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            } else {
                try {
                    this.writeDocument(title, title);
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } else if (inPage && "page".equals(localName)) {
            inPage = false;
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
