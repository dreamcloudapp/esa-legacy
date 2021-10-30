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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateResolver extends XmlWritingHandler {
    protected Pattern templateTextPattern = Pattern.compile("[^{]\\{\\{([^#<>\\[\\]{}|]+)");
    protected final SAXParserFactory saxFactory;
    protected int docsStripped = 0;
    protected int invokes = 0;
    protected int templates = 0;
    ArrayList<Pattern> titleExclusionPatterns;

    public TemplateResolver(TemplateResolutionOptions options) {
        this.setDocumentTag("page");
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
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
        System.out.println("Invokes:\t" + invokes);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Strip Rate:\t" + format.format(((double) docsStripped) / ((double) this.getDocsRead())));
        System.out.println("----------------------------------------");
    }

    public void handleDocument(Map<String, String> xmlFields) {
        int docsRead = this.getDocsRead();
        if (docsRead % 1000 == 0) {
            System.out.println("processed article\t[" + docsStripped + " | " + docsRead + "]");
        }

        String title = xmlFields.get("title");
        String text = xmlFields.get("text");
        /*Matcher textMatcher = templateTextPattern.matcher(text);
        if (textMatcher.find()) {
            System.out.println("template invocation: " + textMatcher.group(1));
        }*/
        if (title.contains("Template:")) {
            templates++;
            if (text.contains("#invoke")) {
                invokes++;
            }
        }


        return;

        /*
        //Write to the file
        try {
            this.writeDocument(StringUtils.normalizeWikiTitle(title), text);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
         */
    }

    public void writeDocument(String title, String text) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        this.writeElement("title", title);
        this.writeElement("text", text);
        this.writeEndElement();
    }
}
