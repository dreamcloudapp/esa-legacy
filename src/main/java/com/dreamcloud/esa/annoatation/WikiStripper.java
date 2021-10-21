package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.tools.BZipFileReader;
import com.dreamcloud.esa.tools.StringUtils;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Takes a Wikimedia dump file and strips out all of the extra information.
 * It also applies basic title exclusions to reduce the file size.
 * Wikipedia redirect articles are further removed.
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
    protected String redirectRegex = "^#REDIRECT \\[\\[(.+)]]$";
    protected Pattern redirectPattern;
    protected final SAXParserFactory saxFactory;
    protected boolean inPage;
    protected boolean inPageTitle;
    protected boolean inPageText;
    protected StringBuilder content = new StringBuilder();
    protected String title;
    protected int numRead = 0;
    protected int numStripped = 0;
    protected XMLStreamWriter xmlWriter;
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
        redirectPattern = Pattern.compile(redirectRegex);
    }

    protected void reset() {
        numRead = 0;
    }

    public void strip(File inputFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        reset();

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
                System.out.println("processed article\t[" + numStripped + " | " + numRead + "]");
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

            //Exclude redirects
            String text = content.toString();
            Matcher matcher = redirectPattern.matcher(text);
            if (matcher.matches()) {
                this.numStripped++;
                return;
            }

            //Write to the file
            try {
                this.writeDocument(StringUtils.normalizeWikiTitle(title), text);
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
