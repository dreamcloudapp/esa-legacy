package be.vanoosten.esa;

import java.util.ArrayList;

public class WikipediaArticleAnalysis {
    public String parsedTitle;
    public ArrayList<String> outgoingLinks;
    public int tokenCount = 0;

    WikipediaArticleAnalysis(String parsedTitle, ArrayList<String> outgoingLinks, int tokenCount) {
        this.parsedTitle = parsedTitle;
        this.outgoingLinks = outgoingLinks;
        this.tokenCount = tokenCount;
    }

    public boolean canIndex() {
        return true;
        //return this.outgoingLinks.size() > 1 && this.tokenCount > 9;
    }
}
