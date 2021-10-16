package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.indexer.WikiIndexerOptions;

import java.util.ArrayList;

public class WikipediaArticleAnalysis {
    public String parsedTitle;
    public ArrayList<String> outgoingLinks;
    public int tokenCount;

    public WikipediaArticleAnalysis(String parsedTitle, ArrayList<String> outgoingLinks, int tokenCount) {
        this.parsedTitle = parsedTitle;
        this.outgoingLinks = outgoingLinks;
        this.tokenCount = tokenCount;
    }

    public boolean canIndex(WikiIndexerOptions options) {
        return this.outgoingLinks.size() >= options.minimumOutgoingLinks && this.tokenCount >= options.minimumTermCount;
    }
}
