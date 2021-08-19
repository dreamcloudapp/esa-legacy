package be.vanoosten.esa;

import java.util.ArrayList;

public class WikipediaArticle {
    public int index;
    public String title;
    public String text;
    public WikipediaArticleAnalysis analysis = null;
    public boolean indexed = false;

    WikipediaArticle(int index, String title, String text) {
        this.index = index;
        this.title = title;
        this.text = text;
    }

    public boolean canIndex() {
        return this.analysis != null && this.analysis.canIndex();
    }

    public ArrayList<String> getOutgoingLinks() {
        if (analysis == null) {
            return new ArrayList<>();
        } else {
            return this.analysis.outgoingLinks;
        }
    }
}
