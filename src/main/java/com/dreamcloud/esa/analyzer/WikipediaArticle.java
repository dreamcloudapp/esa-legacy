package com.dreamcloud.esa.analyzer;

import com.dreamcloud.esa.indexer.WikiIndexerOptions;

public class WikipediaArticle {
    public String title;
    public String text;
    public Integer incomingLinks;
    public Integer outgoingLinks;
    public Integer terms;


    public boolean canIndex(WikiIndexerOptions options) {
        return !(
                (options.minimumIncomingLinks > 0 && incomingLinks != null && incomingLinks < options.minimumIncomingLinks)
                || (options.minimumOutgoingLinks > 0 && outgoingLinks != null && outgoingLinks < options.minimumOutgoingLinks)
                || (options.minimumTermCount > 0 && terms != null && terms < options.minimumTermCount)
                || (options.maximumTermCount > 0 && terms != null && terms > options.maximumTermCount)
        );
    }
}
