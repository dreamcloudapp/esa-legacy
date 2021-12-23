package com.dreamcloud.esa.annoatation;

import org.apache.commons.collections4.MultiValuedMap;

import java.util.Collection;
import java.util.Set;

public class LinkPruner {
    protected MultiValuedMap<String, String> incomingLinkMap;
    protected MultiValuedMap<String, String> outgoingLinkMap;
    protected int minimumLinks = 0;

    public LinkPruner(MultiValuedMap<String, String> incomingLinkMap, MultiValuedMap<String, String> outgoingLinkMap, int minimumLinks) {
        this.incomingLinkMap = incomingLinkMap;
        this.outgoingLinkMap = outgoingLinkMap;
        this.minimumLinks = minimumLinks;
    }

    public Set<String> prune() {
        while (pruneOutgoingLinks() + pruneIncomingLinks() > 0);
        return outgoingLinkMap.keySet();
    }

    protected int pruneOutgoingLinks() {
        return pruneMap(outgoingLinkMap, incomingLinkMap);
    }

    protected int pruneIncomingLinks() {
        return pruneMap(incomingLinkMap, outgoingLinkMap);
    }

    private int pruneMap(MultiValuedMap<String, String> incomingLinkMap, MultiValuedMap<String, String> outgoingLinkMap) {
        int pruned = 0;
        for (String linkedArticle: incomingLinkMap.keySet()) {
            Collection<String> articles = incomingLinkMap.get(linkedArticle);
            if (articles.size() < minimumLinks) {
                incomingLinkMap.remove(linkedArticle);
                for (String article: articles) {
                    outgoingLinkMap.removeMapping(article, linkedArticle);
                }
                pruned++;
            }
        }
        return pruned;
    }
}
