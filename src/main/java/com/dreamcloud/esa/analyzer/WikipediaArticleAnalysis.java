package com.dreamcloud.esa.analyzer;

import java.util.ArrayList;

public class WikipediaArticleAnalysis {
    public String parsedTitle;
    public ArrayList<String> outgoingLinks;
    public int tokenCount;

    WikipediaArticleAnalysis(String parsedTitle, ArrayList<String> outgoingLinks, int tokenCount) {
        this.parsedTitle = parsedTitle;
        this.outgoingLinks = outgoingLinks;
        this.tokenCount = tokenCount;
    }

    public boolean canIndex() {
        return this.outgoingLinks.size() > 0 && this.tokenCount > 9;
    }
}
