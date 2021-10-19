package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.StopWordRepository;

public class AnnotatorOptions {
    public int minimumIncomingLinks = 0;
    public int minimumOutgoingLinks = 0;
    public int minimumTermCount = 0;
    public StopWordRepository stopWordRepository;
}
