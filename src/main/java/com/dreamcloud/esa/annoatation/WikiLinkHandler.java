package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlWritingHandler;
import com.dreamcloud.esa.tools.StringUtils;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLinkHandler extends XmlWritingHandler {
    protected final SAXParserFactory saxFactory;
    protected int numStripped = 0;
    static Pattern linkRegexPattern = Pattern.compile("\\[\\[(?!File:|Image:)([^|#\\]]+)[^]]*]]");
    protected Map<String, String> titleMap;
    protected Map<String, WikiLinkAnnotation> annotations;

    public WikiLinkHandler(Map<String, String> titleMap, Map<String, WikiLinkAnnotation> annotations, File outputFile) throws XMLStreamException, IOException {
        this.titleMap = titleMap;
        this.annotations = annotations;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
        this.open(outputFile);
        this.writeDocumentBegin("docs");
    }

    public void handleDocument(Map<String, String> xmlFields) {
        String title = xmlFields.get("title");
        StringBuilder text = new StringBuilder(xmlFields.get("text"));
        Matcher matcher = linkRegexPattern.matcher(text.toString());
        Set<String> outgoingLinks = new HashSet<>();
        while (matcher.find()) {
            String normalizedLink = StringUtils.normalizeWikiTitle(matcher.group(1));
            if (titleMap.containsKey(normalizedLink)) {
                text.append((" " + normalizedLink).repeat(3));
                outgoingLinks.add(titleMap.get(normalizedLink));
            }
        }
        text.append((" " + title).repeat(4));

        try {
            this.writeStartElement("doc");
            this.writeElement("title", title);
            this.writeElement("text", text.toString());
            this.writeEndElement();
        } catch (XMLStreamException|IOException e) {
            e.printStackTrace();
            System.exit(1);
        }


        //Handle outgoing links and token count
        if (!annotations.containsKey(title)) {
            WikiLinkAnnotation annotation = new WikiLinkAnnotation();
            annotation.outgoingLinks = outgoingLinks.size();
            annotations.put(title, annotation);
        } else {
            WikiLinkAnnotation annotation = annotations.get(title);
            annotation.outgoingLinks = outgoingLinks.size();
        }

        //Handle incoming links
        for (String outgoingLink: outgoingLinks) {
            if (!annotations.containsKey(outgoingLink)) {
                WikiLinkAnnotation annotation = new WikiLinkAnnotation();
                annotation.incomingLinks = 1;
                annotations.put(outgoingLink, annotation);
            } else {
                WikiLinkAnnotation annotation = annotations.get(outgoingLink);
                annotation.incomingLinks++;
            }
        }

        if (this.getDocsRead() % 1000 == 0) {
            System.out.println("link-annotated article\t[" + this.getDocsRead() + "]\t\"" + title + "\"");
        }
    }

    @Override
    public void close() throws Exception {
        writeDocumentEnd();
        super.close();
    }
}
