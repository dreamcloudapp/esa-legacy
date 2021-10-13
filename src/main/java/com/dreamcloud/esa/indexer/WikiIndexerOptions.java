package com.dreamcloud.esa.indexer;

import java.util.ArrayList;

public class WikiIndexerOptions extends IndexerOptions {
    public int minimumIncomingLinks = 0;
    public int minimumOutgoingLinks = 0;
    public ArrayList<String> titleExclusionRegExList;
    public ArrayList<String> titleExclusionList;
}
