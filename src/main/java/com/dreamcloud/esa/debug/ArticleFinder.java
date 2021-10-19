package com.dreamcloud.esa.debug;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Finds an article from a Wikimedia dump file.
 */
public class ArticleFinder extends DefaultHandler {
    File wikimediaDump;
    String searchTitle;
    int searchIndex = -1;

    int numRead = 0;
    boolean foundArticle = false;
    String foundArticleTitle;

    private final SAXParserFactory saxFactory;
    private boolean inPage;
    private boolean inPageTitle;
    private boolean inPageText;
    private StringBuilder content = new StringBuilder();

    public ArticleFinder(File wikimediaDump) {
        this.wikimediaDump = wikimediaDump;
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public DebugArticle find(String articleTitle) {
        this.searchTitle = articleTitle;

        try {
            SAXParser saxParser = saxFactory.newSAXParser();
            InputStream inputStream = new FileInputStream(wikimediaDump);
            inputStream = new BufferedInputStream(inputStream);
            inputStream = new BZip2CompressorInputStream(inputStream, true);
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            InputSource is = new InputSource(reader);
            is.setEncoding("UTF-8");
            saxParser.parse(is, this);
            inputStream.close();
        }
        catch (ArticleFoundException e) {
            return e.article;
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
        }
        return null;
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

    public void endElement(String uri, String localName, String qName) throws ArticleFoundException {
        if (inPage && inPageTitle && "title".equals(localName)) {
            inPageTitle = false;
            String articleTitle = content.toString();
            if (searchTitle.equals(articleTitle) || numRead == searchIndex) {
                foundArticle = true;
                foundArticleTitle = articleTitle;
            }
        } else if (inPage && inPageText && "text".equals(localName)) {
            numRead++;
            if (foundArticle) {
                DebugArticle article = new DebugArticle();
                article.index = numRead;
                article.title = foundArticleTitle;
                article.text = content.toString();
                throw new ArticleFoundException("Article found.", article);
            }
        } else if (inPage && "page".equals(localName)) {
            inPage = false;
        }
    }

    public void characters(char[] ch, int start, int length) {
        if (inPageText || inPageTitle) {
            content.append(ch, start, length);
        }
    }
}
