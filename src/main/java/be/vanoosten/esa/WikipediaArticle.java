package be.vanoosten.esa;

public class WikipediaArticle {
    public String title;
    public int tokenCount;
    public int outgoingLinkCount;

    WikipediaArticle(String title, int tokenCount, int outgoingLinkCount) {
        this.title = title;
        this.tokenCount = tokenCount;
        this.outgoingLinkCount = outgoingLinkCount;
    }
}
