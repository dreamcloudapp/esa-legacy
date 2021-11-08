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
import java.io.*;
import java.util.Map;

public class TemplateResolver extends XmlWritingHandler {
    Map<String, String> templateMap;
    protected final SAXParserFactory saxFactory;
    protected int templates = 0;
    protected TemplateProcessor templateProcessor;

    public TemplateResolver(Map<String, String> templateMap) {
        this.templateMap = templateMap;
        this.setDocumentTag("page");
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void resolve(File inputFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        reset();
        TemplateResolutionOptions options = new TemplateResolutionOptions();
        options.recursionDepth = 1;
        templateProcessor = new TemplateProcessor(templateMap, options);
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        //Begin the XML document
        this.open(outputFile);
        this.writeDocumentBegin("doc");

        saxParser.parse(is, this);
        reader.close();

        //End document
        this.writeDocumentEnd();

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + this.getDocsRead());
        System.out.println("Templates Refs:\t" + templates);
        templateProcessor.displayInfo();
        System.out.println("----------------------------------------");
    }

    public void handleDocument(Map<String, String> xmlFields) {
        int docsRead = this.getDocsRead();
        if (docsRead % 1000 == 0) {
            System.out.println("processed template\t[" + templates + " | " + docsRead + "]");
        }
        String title = xmlFields.get("title");

        //We aren't going to write templates
        if (title.startsWith("Template:")) {
            return;
        }

        String text = xmlFields.get("text");

        try {
            text = templateProcessor.substitute(text, title);
            this.writeDocument(StringUtils.normalizeWikiTitle(title), text);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void writeDocument(String title, String text) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        this.writeElement("title", title);
        this.writeElement("text", text);
        this.writeEndElement();
    }
}
