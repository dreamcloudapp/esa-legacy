package com.dreamcloud.esa.annoatation;

import org.apache.commons.collections4.MultiValuedMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
        int pruned;
        do {
            int prunedOut = pruneOutgoingLinks();
            int prunedIn = pruneIncomingLinks();
            pruned = prunedOut + prunedIn;
            System.out.println("Pruned " + pruned + ": out=" + prunedOut + "; in=" + prunedIn);
        } while (pruned > 0);
        System.out.println("Pruned Outgoing: " + outgoingLinkMap.keySet().size());
        System.out.println("Pruned Incoming: " + incomingLinkMap.keySet().size());
        outgoingLinkMap.keySet().retainAll(incomingLinkMap.keySet());
        System.out.println("Pruned Intersection: " + outgoingLinkMap.keySet().size());
        return outgoingLinkMap.keySet();
    }

    protected int pruneOutgoingLinks() {
        int pruned = 0;
        for (Iterator<String> it = outgoingLinkMap.keySet().iterator(); it.hasNext();) {
            String article = it.next();
            Collection<String> linkedArticles = outgoingLinkMap.get(article);
            if (linkedArticles.size() < minimumLinks) {
                for (String linkedArticle: linkedArticles) {
                    incomingLinkMap.removeMapping(linkedArticle, article);
                }
                it.remove();
                pruned++;
            }
        }
        return pruned;
    }

    protected int pruneIncomingLinks() {
        int pruned = 0;
        for (Iterator<String> it = incomingLinkMap.keySet().iterator(); it.hasNext();) {
            String linkedArticle = it.next();
            Collection<String> articles = incomingLinkMap.get(linkedArticle);
            if (articles.size() < minimumLinks) {
                for (String article: articles) {
                    outgoingLinkMap.removeMapping(article, linkedArticle);
                }
                it.remove();
                pruned++;
            }
        }
        return pruned;
    }
}
