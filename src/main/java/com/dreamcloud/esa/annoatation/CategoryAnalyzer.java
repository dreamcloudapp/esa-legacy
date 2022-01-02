package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlReadingHandler;
import com.dreamcloud.esa.tools.BZipFileReader;
import com.dreamcloud.esa.tools.StringUtils;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.xpath.operations.Bool;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.list.mutable.MutableIterator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CategoryAnalyzer extends XmlReadingHandler {
    protected final SAXParserFactory saxFactory;
    static Pattern categoryRegexPattern = Pattern.compile("\\[\\[\\s?([cC]ategory:[^|#\\]]+)[^]]*]]");
    protected MultiValuedMap<String, String> categoryHierarchy = new HashSetValuedHashMap<>();
    protected MutableObjectIntMap<String> categoryInfo = ObjectIntMaps.mutable.empty();
    Set<String> categories = new HashSet<>();

    public Set<String> getGabrilovichExclusionCategories() {
        categories.add(StringUtils.normalizeWikiTitle("Category:Star name disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:America"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Disambiguation"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Georgia"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Lists of political parties by generic name"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Galaxy name disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Lists of two-letter combinations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Disambiguation categories"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Towns in Italy (StringUtils.normalizeWikiTitle(disambiguation)"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Redirects to disambiguation pages"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Birmingham"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Mathematical disambiguation"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Public schools in Montgomery County"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Structured lists"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Identical titles for unrelated songs"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Signpost articles"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Township disambiguation"));
        categories.add(StringUtils.normalizeWikiTitle("Category:County disambiguation"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Disambiguation pages in need of cleanup"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Human name disambiguation"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Number disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Letter and number combinations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:4-letter acronyms"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Acronyms that may need to be disambiguated"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Lists of roads sharing the same title"));
        categories.add(StringUtils.normalizeWikiTitle("Category:List disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:3-digit Interstate disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Geographical locations sharing the same title"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Tropical cyclone disambiguation"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Repeat-word disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Song disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Disambiguated phrases"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Subway station disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Lists of identical but unrelated album titles"));
        categories.add(StringUtils.normalizeWikiTitle("Category:5-letter acronyms"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Three-letter acronym disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Miscellaneous disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Two-letter acronym disambiguations"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Days"));
        categories.add(StringUtils.normalizeWikiTitle("Category:Eastern Orthodox liturgical days"));
        return categories;
    }

    public CategoryAnalyzer() {
        setDocumentTag("page");
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void analyze(File inputFile) throws ParserConfigurationException, SAXException, IOException {
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");
        saxParser.parse(is, this);
        reader.close();

        /*System.out.println("Category Stats");
        System.out.println("---------------------------------------");
        System.out.println("Total categories " + categoryInfo.size());
        Iterator<String> orderedCategories = categoryInfo.keySet().stream().
                sorted((String k1, String k2) -> (int) Math.signum(categoryInfo.get(k2) - categoryInfo.get(k1))).
                iterator();
        int i = 0;
        while (orderedCategories.hasNext()) {
            String category = orderedCategories.next();
            int count = categoryInfo.get(category);
            System.out.println(category + ":\t\t" + count);
            if (++i == 100) {
                break;
            }
        }

        int excludedCount = 0;

        for (String category: categoryInfo.keySet()) {
            for (String excludedCategory : getGabrilovichExclusionCategories()) {
                if (areCategoriesRelated(excludedCategory, category)) {
                    excludedCount += categoryInfo.get(excludedCategory);
                    break;
                }
            }
        }
        System.out.println("Excluded articles: " + excludedCount);
        System.out.println("---------------------------------------");
         */
        System.out.println("Disambiguation categories:");
        System.out.println("---------------------------------------");
        listCategoryChildren("category:disambiguation");
        System.out.println("---------------------------------------");
    }

    public void listCategoryChildren(String category) {
        Collection<String> childCategories = categoryHierarchy.get(category);
        if (childCategories != null) {
            for (String childCategory: childCategories) {
                categories.add(childCategory);
                System.out.println(childCategory);
                listCategoryChildren(childCategory);
                System.out.println("---");
            }
        }
    }

    protected void handleDocument(Map<String, String> xmlFields) {
        String title = StringUtils.normalizeWikiTitle(xmlFields.get("title"));
        String text = xmlFields.get("text");
        boolean isCategoryArticle = title.startsWith("category:");
        for (String category: getArticleCategories(text)) {
            categoryInfo.addToValue(category, 1);
            if (isCategoryArticle) {
                categoryHierarchy.put(category, title);
            }
        }
        this.logMessage("Categorized article\t" + this.getDocsRead());
    }

    public Collection<String> getArticleCategories(String articleText) {
        Matcher matcher = categoryRegexPattern.matcher(articleText);
        Set<String> categories = new HashSet<>();
        while (matcher.find()) {
            String category = StringUtils.normalizeWikiTitle(matcher.group(1));
            categories.add(category);
        }
        return categories;
    }

    public boolean articleHasCategory(String articleText, String category) {
        for (String articleCategory: getArticleCategories(articleText)) {
            if (areCategoriesRelated(category, articleCategory)) {
                return true;
            }
        }
        return false;
    }

    protected boolean areCategoriesRelated(String parent, String orphan) {
        Set<String> categoriesSeen = new HashSet<>();
        return areCategoriesRelated(parent, orphan, categoriesSeen);
    }

    protected boolean areCategoriesRelated(String parent, String orphan, Set<String> categoriesSeen) {
        if (categoriesSeen.contains(parent)) {
            //prevent recursion
            return false;
        }
        categoriesSeen.add(parent);

        if (parent.equals(orphan)) {
            //as related as you can be
            return true;
        }

        Collection<String> children = categoryHierarchy.get(parent);
        for (String child: children) {
            if (areCategoriesRelated(child, orphan, categoriesSeen)) {
                return true;
            }
        }
        return false;
    }
}
