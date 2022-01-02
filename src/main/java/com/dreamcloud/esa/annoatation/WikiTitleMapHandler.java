package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import java.util.Map;

public class WikiTitleMapHandler extends XmlReadingHandler {
    protected Map<String, String> titleMap;

   public WikiTitleMapHandler(Map<String, String> titleMap) {
       this.titleMap = titleMap;
   }

    protected void handleDocument(Map<String, String> xmlFields) {
        String title = xmlFields.get("title");
        String redirect = xmlFields.get("redirect");
        this.titleMap.put(title, redirect);
    }
}
