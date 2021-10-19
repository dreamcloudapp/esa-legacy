package com.dreamcloud.esa.annoatation;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WikiLinkHandler extends DefaultHandler {
    protected Map<String, String> titleMap;
    protected Map<String, WikiAnnotation> annotations;
    protected MutableObjectIntMap<String> incomingLinks = ObjectIntMaps.mutable.empty();
    protected MutableObjectIntMap<String> outgoingLinks = ObjectIntMaps.mutable.empty();
    protected Analyzer analyzer;

    protected boolean inDoc;
    protected boolean inDocTitle;
    protected boolean inDocText;
    protected StringBuilder content = new StringBuilder();
    protected String title;

    public WikiLinkHandler(Map<String, String> titleMap, Map<String, WikiAnnotation> annotations, Analyzer analyzer) {
        this.titleMap = titleMap;
        this.annotations = annotations;
        this.analyzer = analyzer;
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
            inDocText = false;
            String text = content.toString();
            Set<String> articleOutgoingLinks = new HashSet<>();
            TokenStream tokenStream = analyzer.tokenStream("text", "[[" + title + "]] " + text);
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAttribute = tokenStream.addAttribute(TypeAttribute.class);
            try {
                tokenStream.reset();
                int tokenCount = 0;
                String articleTitleToken = null;
                while(tokenStream.incrementToken()) {
                    if (articleTitleToken == null) {
                        articleTitleToken = termAttribute.toString();
                        continue;
                    }
                    tokenCount++;
                    if (WikipediaTokenizer.INTERNAL_LINK.equals(typeAttribute.type())) {
                        articleOutgoingLinks.add(getRedirectedTitle(termAttribute.toString()));
                    }
                }
                tokenStream.close();

                //Handle outgoing links and token count
                if (!annotations.containsKey(articleTitleToken)) {
                    WikiAnnotation annotation = new WikiAnnotation();
                    annotation.tokens = tokenCount;
                    annotation.outgoingLinks = articleOutgoingLinks.size();
                    annotations.put(articleTitleToken, annotation);
                } else {
                    WikiAnnotation annotation = annotations.get(articleTitleToken);
                    annotation.tokens = tokenCount;
                    annotation.outgoingLinks = articleOutgoingLinks.size();
                }

                //Handle incoming links
                for (String outgoingLink: articleOutgoingLinks) {
                    if (!annotations.containsKey(outgoingLink)) {
                        WikiAnnotation annotation = new WikiAnnotation();
                        annotation.incomingLinks = 1;
                        annotations.put(outgoingLink, annotation);
                    } else {
                        WikiAnnotation annotation = annotations.get(outgoingLink);
                        annotation.incomingLinks++;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else if (inDoc && "doc".equals(localName)) {
            inDoc = false;
        }
    }

    protected String getRedirectedTitle(String title) {
        return titleMap.getOrDefault(title, title);
    }

    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }
}
