package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.tools.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLinkHandler extends DefaultHandler {
    static Pattern linkRegexPattern = Pattern.compile("\\[\\[(?!File:|Image:)([^|#\\]]+)[^]]*]]");
    protected Map<String, String> titleMap;
    protected Map<String, WikiLinkAnnotation> annotations;

    protected boolean inDoc;
    protected boolean inDocTitle;
    protected boolean inDocText;
    protected StringBuilder content = new StringBuilder();
    protected String title;
    public int numRead = 0;

    public WikiLinkHandler(Map<String, String> titleMap, Map<String, WikiLinkAnnotation> annotations) {
        this.titleMap = titleMap;
        this.annotations = annotations;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("doc".equals(localName)) {
            inDoc = true;
        } else if (inDoc && "title".equals(localName)) {
            inDocTitle = true;
            content = new StringBuilder();
        } else if (inDoc && "text".equals(localName)) {
            inDocText = true;
            content = new StringBuilder();
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (inDoc && inDocTitle && "title".equals(localName)) {
            inDocTitle = false;
            title = content.toString();
        } else if (inDoc && inDocText && "text".equals(localName)) {
            numRead++;

            inDocText = false;
            String text = content.toString();
            Matcher matcher = linkRegexPattern.matcher(text);
            Set<String> outgoingLinks = new HashSet<>();
            while (matcher.find()) {
                String normalizedLink = StringUtils.normalizeWikiTitle(matcher.group(1));
                if (titleMap.containsKey(normalizedLink)) {
                    outgoingLinks.add(titleMap.get(normalizedLink));
                }
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

            if (numRead % 1000 == 0) {
                System.out.println("link-annotated article\t[" + numRead + "]\t\"" + title + "\"");
            }
        } else if (inDoc && "doc".equals(localName)) {
            inDoc = false;
        }
    }

    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }
}
