package com.dreamcloud.esa.indexer;

import java.util.ArrayList;

public class WikiIndexerOptions extends IndexerOptions {
    public int minimumIncomingLinks = 0;
    public int minimumOutgoingLinks = 0;
    public ArrayList<String> titleExclusionRegExList;
    public ArrayList<String> titleExclusionList;

    public WikiIndexerOptions() {
        this.titleExclusionRegExList = new ArrayList<>();
        this.titleExclusionList = new ArrayList<>();
    }

    public void displayInfo() {
        super.displayInfo();

        System.out.println("Wiki-specific indexing options:");
        System.out.println("---------------------------------------");
        System.out.println("Excluded Titles\t" + String.join(", ", titleExclusionRegExList));
        System.out.println("Excluded Title Patterns\t" + String.join(", ", titleExclusionRegExList));
        System.out.println("---------------------------------------");
    }
}
