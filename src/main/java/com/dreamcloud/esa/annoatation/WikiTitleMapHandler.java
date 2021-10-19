package com.dreamcloud.esa.annoatation;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Map;

public class WikiTitleMapHandler extends DefaultHandler {
    protected Map<String, String> titleMap;

    protected boolean inDoc;
    protected boolean inDocTitle;
    protected boolean inDocRedirect;
    protected StringBuilder content = new StringBuilder();
    protected String title;

   public WikiTitleMapHandler(Map<String, String> titleMap) {
       this.titleMap = titleMap;
   }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("doc".equals(localName)) {
            inDoc = true;
        } else if (inDoc && "title".equals(localName)) {
            inDocTitle = true;
            content = new StringBuilder();
        } else if (inDoc && "redirect".equals(localName)) {
            inDocRedirect = true;
            content = new StringBuilder();
        }
    }

    public void endElement(String uri, String localName, String qName) {
        if (inDoc && inDocTitle && "title".equals(localName)) {
            inDocTitle = false;
            title = content.toString();
        } else if (inDoc && inDocRedirect && "redirect".equals(localName)) {
            inDocRedirect = false;
            titleMap.put(title, content.toString());
        } else if (inDoc && "doc".equals(localName)) {
            inDoc = false;
        }
    }

    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }
}
