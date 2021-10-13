package com.dreamcloud.esa.indexer;

import java.util.ArrayList;

public class WikiIndexerOptions {
    public int minimumExternalLinks = 0;
    public int minimumInternalLinks = 0;
    public int minimumTermCount = 0;
    public int maximumTermCount = 0;
    public ArrayList<String> titleExclusionRegExList;
    public ArrayList<String> titleExclusionList;
}
