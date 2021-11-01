package com.dreamcloud.esa;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.dreamcloud.esa.annoatation.TemplateProcessor;
import com.dreamcloud.esa.annoatation.TemplateResolutionOptions;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;



public class TemplateProcessorTest {
    TemplateProcessor processor;

    public TemplateProcessorTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() {

    }

    private TemplateProcessor getDefaultProcessor() {
        TemplateResolutionOptions options = new TemplateResolutionOptions();
        Map<String, String> templateMap = new HashMap<>();
        return new TemplateProcessor(templateMap, options);
    }

    @Test
    public void testInstantiation() {
        TemplateProcessor processor = getDefaultProcessor();
        assertEquals(processor, processor);
    }

    @Test
    public void testSimpleTemplate() throws IOException {
        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("foo", "bar");

        TemplateResolutionOptions options = new TemplateResolutionOptions();
        TemplateProcessor processor = new TemplateProcessor(templateMap, options);

        String article = "{{foo}}";
        String processedArticle = processor.substitute(article);
        assertEquals(processedArticle, "bar");
    }

    @Test
    public void testTemplateWithParameter() throws IOException {
        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("foo", "hello {{{1}}}");

        TemplateResolutionOptions options = new TemplateResolutionOptions();
        TemplateProcessor processor = new TemplateProcessor(templateMap, options);

        String article = "{{foo|world}}";
        String processedArticle = processor.substitute(article);
        assertEquals(processedArticle, "hello world");
    }

    @Test
    public void testTemplateWithNamedParameter() throws IOException {
        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("foo", "hello {{{bar}}}");

        TemplateResolutionOptions options = new TemplateResolutionOptions();
        TemplateProcessor processor = new TemplateProcessor(templateMap, options);

        String article = "{{foo|bar=world}}";
        String processedArticle = processor.substitute(article);
        assertEquals(processedArticle, "hello world");
    }

    @Test
    public void testTemplateWithAllParameters() throws IOException {
        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("foo", "{{{1}}} {{{bar}}}{{{3}}}");

        TemplateResolutionOptions options = new TemplateResolutionOptions();
        TemplateProcessor processor = new TemplateProcessor(templateMap, options);

        String article = "{{foo|hello|bar=world|!}}";
        String processedArticle = processor.substitute(article);
        assertEquals(processedArticle, "hello world!");
    }
}
