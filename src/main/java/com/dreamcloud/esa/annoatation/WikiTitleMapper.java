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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Map;
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
class WikiTitleMapper extends XmlWritingHandler {
    ArrayList<Pattern> titleExclusionPatterns;
    protected Pattern redirectPattern = Pattern.compile("^.*#REDIRECT[^\\[]*\\[\\[([^#]+)(#.+)?]]", Pattern.CASE_INSENSITIVE);    protected File inputFile;
    protected final SAXParserFactory saxFactory;
    protected int numRedirects = 0;

    protected WikiTitleMapper(ArrayList<Pattern> titleExclusionPatterns, File inputFile) {
        this.setDocumentTag("page");
        this.inputFile = inputFile;
        this.titleExclusionPatterns = titleExclusionPatterns;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void mapToXml(File outputFile) throws IOException, XMLStreamException, ParserConfigurationException, SAXException {
        this.open(outputFile);
        this.writeDocumentBegin("docs");
        this.parse();
        this.writeDocumentEnd();

        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + getDocsRead());
        System.out.println("Articles Redirected:\t" + numRedirects);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Redirection Rate:\t" + format.format(((double) numRedirects) / ((double) getDocsRead())));
        System.out.println("----------------------------------------");
    }

    protected void parse() throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, this);
        reader.close();
    }

    @Override
    protected void handleDocument(Map<String, String> xmlFields) {
        String title = StringUtils.normalizeWikiTitle(xmlFields.get("title"));
        //Exclude titles by regex
        for (Pattern pattern: this.titleExclusionPatterns) {
            Matcher matcher = pattern.matcher(title.toLowerCase());
            if (matcher.find()) {
                return;
            }
        }
        String text = xmlFields.get("text");
        String redirect = title;
        Matcher matcher = redirectPattern.matcher(text);
        if (matcher.matches()) {
            numRedirects++;
            redirect = StringUtils.normalizeWikiTitle(matcher.group(1));
            for (Pattern pattern: this.titleExclusionPatterns) {
                Matcher redirectMatcher = pattern.matcher(redirect.toLowerCase());
                if (redirectMatcher.find()) {
                    return;
                }
            }
        }
        try {
            this.writeDocument(title, redirect);
            this.logMessage("processed article\t[" + numRedirects + " | " + getDocsRead() + "]");
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    protected void writeDocument(String title, String redirect) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        this.writeElement("title", title);
        this.writeElement("redirect", redirect);
        this.writeEndElement();
    }
}
