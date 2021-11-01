package com.dreamcloud.esa;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;

import com.dreamcloud.esa.parser.TemplateParser;
import com.dreamcloud.esa.parser.TemplateReference;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;



public class TemplateParserTest {
    public TemplateParserTest() {
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

    private ArrayList<TemplateReference> quickParse(String text) throws IOException {
        TemplateParser parser = new TemplateParser();
        PushbackReader reader = new PushbackReader(new StringReader(text));
        return parser.parseTemplates(reader);
    }

    @Test
    public void testInstantiation() {
        new TemplateParser();
    }

    @Test
    public void testSimpleTemplate() throws IOException {
        TemplateParser parser = new TemplateParser();
        ArrayList<TemplateReference> templates = this.quickParse("{{{foo}}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("foo", template.name);
        assertEquals(0, template.parameters.size());
        assertEquals("{{{foo}}}", template.text);
    }
}
