package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.tools.StringUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiLinAndTermHandler extends XmlReadingHandler {
    public static int ANALYSIS_TERMS = 1;
    public static int ANALYSIS_LINKS = 2;

    WikiLinkAndTermAnnotatorOptions options;
    protected int numStripped = 0;
    static Pattern linkRegexPattern = Pattern.compile("\\[\\[(?!File:|Image:)([^|#\\]]+)[^]]*]]");
    protected Map<String, String> titleMap;
    protected MultiValuedMap<String, String> incomingLinkMap;
    protected MultiValuedMap<String, String> outgoingLinkMap;
    protected Map<String, WikiAnnotation> annotations;
    protected int analysisMode;

    public WikiLinAndTermHandler(WikiLinkAndTermAnnotatorOptions options, Map<String, String> titleMap, Map<String, WikiAnnotation> annotations, int analysisMode) {
        this.options = options;
        this.titleMap = titleMap;
        this.annotations = annotations;
        this.analysisMode = analysisMode;
    }

    public WikiLinAndTermHandler(WikiLinkAndTermAnnotatorOptions options, Map<String, String> titleMap, Map<String, WikiAnnotation> annotations, int analysisMode, MultiValuedMap<String, String> incomingLinks, MultiValuedMap<String, String> outgoingLinks) {
        this.options = options;
        this.titleMap = titleMap;
        this.annotations = annotations;
        this.analysisMode = analysisMode;
        this.incomingLinkMap = incomingLinks;
        this.outgoingLinkMap = outgoingLinks;
    }

    protected void handleDocument(Map<String, String> xmlFields) {
        String title = xmlFields.get("title");
        String text = xmlFields.get("text");

        //Analyze the text!
        if (analysisMode == ANALYSIS_TERMS) {
            int termCount = 0;
            try {
                TokenStream tokens = options.analyzer.tokenStream("text", text);
                tokens.addAttribute(CharTermAttribute.class);
                tokens.reset();
                while(tokens.incrementToken()) {
                    termCount++;
                }
                tokens.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            if (termCount >= options.minimumTerms) {
                annotations.put(title, new WikiAnnotation(0, 0, termCount));
            }
        } else {
            WikiAnnotation annotation = annotations.get(title);
            if (annotation == null) {
                return;
            }

            /*
                Create a set of outgoing links to valid annotations.
                We will add to our outgoing link count for each valid link in the set.
                Valid annotations were added to the map if they had enough terms.
                After that, we go to each annotation for an outgoing link and up it's incoming link count.
             */
            Matcher matcher = linkRegexPattern.matcher(text);
            Set<String> outgoingLinks = new HashSet<>();
            while (matcher.find()) {
                String link = titleMap.get(StringUtils.normalizeWikiTitle(matcher.group(1)));
                if (link != null && annotations.containsKey(link)) {
                    outgoingLinks.add(link);
                }
            }

            //Add to our outgoing links
            annotation.outgoingLinks = outgoingLinks.size();

            //Add to others incoming links
            for (String outgoingLink: outgoingLinks) {
                if (annotations.containsKey(outgoingLink)) {
                    WikiAnnotation outgoingAnnotation = annotations.get(outgoingLink);
                    outgoingAnnotation.incomingLinks++;

                    outgoingLinkMap.put(title, outgoingLink);
                    incomingLinkMap.put(outgoingLink, title);
                }
            }
        }

        if (this.getDocsRead() % 1000 == 0) {
            System.out.println((analysisMode == ANALYSIS_LINKS ? "link" : "term") + "-annotated article\t[" + this.getDocsRead() + "]\t\"" + title + "\"");
        }
    }
}
