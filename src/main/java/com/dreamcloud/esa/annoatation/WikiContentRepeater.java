package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlWritingHandler;
import com.dreamcloud.esa.tools.BZipFileReader;
import com.dreamcloud.esa.tools.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiContentRepeater extends XmlWritingHandler {
    protected final SAXParserFactory saxFactory;
    private final WikiContentRepeatOptions options;
    static Pattern linkRegexPattern = Pattern.compile("\\[\\[(?!File:|Image:)([^|#\\]]+)[^]]*]]");

    public WikiContentRepeater(WikiContentRepeatOptions options) {
        this.options = options;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    protected void handleDocument(Map<String, String> xmlFields) throws SAXException {
        String title = xmlFields.get("title");
        StringBuilder text = new StringBuilder(xmlFields.get("text"));
        if (options.linkRepeat > 0) {
            Matcher matcher = linkRegexPattern.matcher(text.toString());
            while (matcher.find()) {
                String normalizedLink = StringUtils.normalizeWikiTitle(matcher.group(1));
                text.append((" " + normalizedLink).repeat(options.linkRepeat));
            }
        }

        if (options.titleRepeat > 0) {
            text.append((" " + title).repeat(options.titleRepeat));
        }

        xmlFields.put("text", text.toString());

        try {
            writeDocument(xmlFields);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (this.getDocsRead() % 1000 == 0) {
            System.out.println("content repeated \t[" + this.getDocsRead() + "]");
        }
    }

    public void writeDocument(Map<String, String> xmlFields) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        for(Map.Entry<String, String> xmlField: xmlFields.entrySet()) {
            this.writeElement(xmlField.getKey(), xmlField.getValue());
        }
        this.writeEndElement();
    }

    public void repeatContent(File inputFile, File outputFile) throws ParserConfigurationException, SAXException, XMLStreamException, IOException {
        System.out.println("Content Repeat:");
        System.out.println("---------------------------------------");
        System.out.println("Link Repeat: " + options.linkRepeat);
        System.out.println("Title Repeat: " + options.titleRepeat);
        System.out.println("---------------------------------------");

        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        this.open(outputFile);
        this.writeDocumentBegin("docs");
        saxParser.parse(is, this);
        reader.close();
        this.writeDocumentEnd();
    }
}
