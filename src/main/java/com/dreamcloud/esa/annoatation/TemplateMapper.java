package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.analyzer.AnalyzerOptions;
import com.dreamcloud.esa.analyzer.EsaAnalyzer;
import com.dreamcloud.esa.analyzer.TokenizerFactory;
import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.tools.BZipFileReader;
import com.dreamcloud.esa.tools.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TemplateMapper extends XmlReadingHandler {
    private static final WikiTitleMatcher matcher = WikiTitleMatcher.createForTemplateStripping();
    private static final Pattern noIncludeTagPattern = Pattern.compile("<noinclude>.+</noinclude>", Pattern.DOTALL);
    private static final Pattern onlyIncludeTagPattern = Pattern.compile("<onlyinclude>(.+)</onlyinclude>", Pattern.DOTALL);

    Analyzer analyzer;
    TemplateResolutionOptions options;
    Map<String, String> templateMap;
    protected final SAXParserFactory saxFactory;
    protected int docsStripped = 0;
    protected int invokes = 0;
    protected int templates = 0;

    public TemplateMapper(TemplateResolutionOptions options) {
        AnalyzerOptions analyzerOptions = new AnalyzerOptions();
        analyzerOptions.tokenizerFactory = new TokenizerFactory() {
            public Tokenizer getTokenizer() {
                return new WikipediaTokenizer();
            }
        };
        analyzer = new EsaAnalyzer(analyzerOptions);
        this.options = options;
        this.setDocumentTag("page");
        this.allowArticleTag("onlyinclude");
        this.allowArticleTag("noinclude");
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void reset() {
        super.reset();
        templateMap = new HashMap<>();
        docsStripped = 0;
        invokes = 0;
        templates = 0;
    }

    public Map<String, String> map(File inputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        reset();
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        saxParser.parse(is, this);
        reader.close();

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + this.getDocsRead());
        System.out.println("Articles Stripped:\t" + docsStripped);
        System.out.println("Invokes:\t" + invokes);
        System.out.println("Templates:\t" + templates);
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMinimumFractionDigits(1);
        System.out.println("Strip Rate:\t" + format.format(((double) docsStripped) / ((double) this.getDocsRead())));
        System.out.println("----------------------------------------");

        //Preprocess the templates to save on performance
        TemplateProcessor processor = new TemplateProcessor(templateMap, options);
        int preprocessCount = 0;
        for (Map.Entry<String, String> entry: templateMap.entrySet()) {
            ArrayList<String> templatesSeen = new ArrayList<>();
            templatesSeen.add(entry.getKey());
            String resolvedText = processor.substitute(entry.getValue(), entry.getKey(), templatesSeen);
            System.out.println("preprocessed template:\t" + (preprocessCount++) + "\t" + entry.getKey());
            templateMap.put(entry.getKey(), resolvedText);
        }
        processor.displayInfo();
        return templateMap;
    }

    public void handleDocument(Map<String, String> xmlFields) {
        int docsRead = this.getDocsRead();
        if (docsRead % 1000 == 0) {
            System.out.println("processed template\t[" + docsStripped + " | " + templates + "]");
        }

        String title = xmlFields.get("title");

        if (title.startsWith("Template:")) {
            templates++;

            if (matcher.matches(title)) {
                docsStripped++;
                return;
            }

            title = title.substring(9);
            title = StringUtils.normalizeWikiTitle(title);
            String text = xmlFields.get("text");

            if (text.contains("#invoke")) {
                invokes++;
                docsStripped++;
                return;
            }

            //Get the valid template text
            if (text.contains("<noinclude>")) {
                //strip out the tag
                Matcher noIncludeMatcher = noIncludeTagPattern.matcher(text);
                text = noIncludeMatcher.replaceAll("");
            }
            if (text.contains("<onlyinclude>")) {
                Matcher onlyIncludeMatcher = onlyIncludeTagPattern.matcher(text);
                StringBuilder templateBuilder = new StringBuilder();
                while (onlyIncludeMatcher.find()) {
                    templateBuilder.append(onlyIncludeMatcher.group(1)).append(' ');
                }
                text = templateBuilder.toString();
            }

            //Ensure that the template is long enough
            if (options.minimumTerms > 0) {
                TokenStream tokenStream = analyzer.tokenStream("text", text);
                CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
                int tokenCount = 0;
                try {
                    tokenStream.reset();
                    while(tokenStream.incrementToken()) {
                        tokenCount++;
                    }
                    tokenStream.close();
                    if (tokenCount < options.minimumTerms) {
                        docsStripped++;
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }

            templateMap.put(title, text);
        }
    }
}
