package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.tools.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLinkHandler extends XmlReadingHandler {
    protected int numStripped = 0;
    static Pattern linkRegexPattern = Pattern.compile("\\[\\[(?!File:|Image:)([^|#\\]]+)[^]]*]]");
    protected Map<String, String> titleMap;
    protected Map<String, WikiLinkAnnotation> annotations;

    public WikiLinkHandler(Map<String, String> titleMap, Map<String, WikiLinkAnnotation> annotations) {
        this.titleMap = titleMap;
        this.annotations = annotations;
    }

    public void handleDocument(Map<String, String> xmlFields) {
        String title = xmlFields.get("title");
        String text = xmlFields.get("text");
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

        if (this.getDocsRead() % 1000 == 0) {
            System.out.println("link-annotated article\t[" + this.getDocsRead() + "]\t\"" + title + "\"");
        }
    }
}
