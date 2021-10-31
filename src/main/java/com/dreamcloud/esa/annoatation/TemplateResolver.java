package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlWritingHandler;
import com.dreamcloud.esa.parser.TemplateParser;
import com.dreamcloud.esa.parser.TemplateReference;
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
import java.io.StringReader;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;

public class TemplateResolver extends XmlWritingHandler {
    TemplateResolutionOptions options;
    Map<String, String> templateMap;
    protected final SAXParserFactory saxFactory;
    protected int docsStripped = 0;
    protected int templates = 0;

    public TemplateResolver(TemplateResolutionOptions options, Map<String, String> templateMap) {
        this.options = options;
        this.templateMap = templateMap;
        this.setDocumentTag("page");
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void reset() {
        super.reset();
        docsStripped = 0;
    }

    public void resolve(File inputFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        reset();
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
        System.out.println("Articles Stripped:\t" + docsStripped);
        System.out.println("Templates:\t" + templates);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Strip Rate:\t" + format.format(((double) docsStripped) / ((double) this.getDocsRead())));
        System.out.println("----------------------------------------");
    }

    public void handleDocument(Map<String, String> xmlFields) {
        int docsRead = this.getDocsRead();
        if (docsRead % 1000 == 0) {
            System.out.println("processed template\t[" + docsStripped + " | " + templates + "]");
        }
        String title = xmlFields.get("title");
        String text = xmlFields.get("text");

        try {
            this.writeDocument(StringUtils.normalizeWikiTitle(title), text);
            text = this.resolveTemplates(text, templateMap);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String resolveTemplates(String text, Map<String, String> templateMap) throws IOException {
        TemplateParser parser = new TemplateParser();
        ArrayList<TemplateReference> templateReferences = parser.parseTemplates(
            new StringReader(text)
        );
        this.logMessage("found " + templateReferences.size() + " templates");
        return text;
    }

    public void writeDocument(String title, String text) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        this.writeElement("title", title);
        this.writeElement("text", text);
        this.writeEndElement();
    }
}
