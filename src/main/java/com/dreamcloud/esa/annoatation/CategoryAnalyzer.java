package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.tools.BZipFileReader;
import com.dreamcloud.esa.tools.StringUtils;

import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoryAnalyzer extends XmlReadingHandler {
    protected final SAXParserFactory saxFactory;
    boolean firstPassComplete = false;
    static Pattern categoryRegexPattern = Pattern.compile("\\[\\[Category:\\s*([^|#\\]]+)[^]]*]]");
    protected Map<String, String> categoryHierarchy = new HashMap<>();
    protected MutableObjectIntMap<String> categoryInfo = ObjectIntMaps.mutable.empty();

    public CategoryAnalyzer() {
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void analyze(File inputFile) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, this);
        reader.close();

        System.out.println("Category Stats");
        System.out.println("---------------------------------------");
        System.out.println("Total categories " + categoryInfo.size());
        for (String category: categoryInfo.keySet()) {
            int count = categoryInfo.get(category);
            System.out.println(category + ":\t\t" + count);
        }
        System.out.println("---------------------------------------");
    }

    public void handleDocument(Map<String, String> xmlFields) {
        String text = xmlFields.get("text");
        Matcher matcher = categoryRegexPattern.matcher(text);
        Set<String> categories = new HashSet<>();
        while (matcher.find()) {
            String category = StringUtils.normalizeWikiTitle(matcher.group(1));
            categories.add(category);
        }
        for (String category: categories) {
            categoryInfo.addToValue(category, 1);
        }
        this.logMessage("Analyzed article\t" + this.getDocsRead());
    }
}
