package com.dreamcloud.esa;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;

import com.dreamcloud.esa.parser.TemplateParameter;
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
        return parser.parse(reader);
    }

    private void assertParameter(ArrayList<TemplateParameter> parameters, int index, String name, String value) {
        TemplateParameter parameter = parameters.get(index - 1);
        assertNotNull(parameter);
        assertEquals(parameter.index, index);
        assertEquals(name, parameter.name);
        assertEquals(value, parameter.value);
    }

    @Test
    public void testInstantiation() {
        new TemplateParser();
    }

    @Test
    public void testSimpleTemplate() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{foo}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("foo", template.name);
        assertEquals(0, template.parameters.size());
        assertEquals("{{foo}}", template.text);
    }

    @Test
    public void testNumberedParameters() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{foo|bar|baz}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("foo", template.name);
        assertEquals(2, template.parameters.size());
        assertEquals("{{foo|bar|baz}}", template.text);

        assertParameter(template.parameters, 1, "1", "bar");
        assertParameter(template.parameters, 2, "2", "baz");
    }

    @Test
    public void testNamedParameters() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{foo|bar=baz|baz=bar}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("foo", template.name);
        assertEquals(2, template.parameters.size());
        assertEquals("{{foo|bar=baz|baz=bar}}", template.text);

        assertParameter(template.parameters, 1, "bar", "baz");
        assertParameter(template.parameters, 2, "baz", "bar");
    }

    @Test
    public void testNamedAndNumberedParameters() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{foo|bar=baz|hello|baz=bar|world}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("foo", template.name);
        assertEquals(4, template.parameters.size());
        assertEquals("{{foo|bar=baz|hello|baz=bar|world}}", template.text);

        assertParameter(template.parameters, 1, "bar", "baz");
        assertParameter(template.parameters, 2, "2", "hello");
        assertParameter(template.parameters, 3, "baz", "bar");
        assertParameter(template.parameters, 4, "4", "world");
    }

    @Test
    public void testNestedTemplate() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{foo|bar=baz|hello|baz=bar|{{world|test}}}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("foo", template.name);
        assertEquals(4, template.parameters.size());
        assertEquals("{{foo|bar=baz|hello|baz=bar|{{world|test}}}}", template.text);

        assertParameter(template.parameters, 1, "bar", "baz");
        assertParameter(template.parameters, 2, "2", "hello");
        assertParameter(template.parameters, 3, "baz", "bar");
        assertParameter(template.parameters, 4, "4", "{{world|test}}");
    }

    @Test
    public void testSpacingAndTrimming() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{ foo | bar = baz | hello | baz=bar | world}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("foo", template.name);
        assertEquals(4, template.parameters.size());
        assertEquals("{{ foo | bar = baz | hello | baz=bar | world}}", template.text);

        assertParameter(template.parameters, 1, "bar", "baz");
        assertParameter(template.parameters, 2, "2", " hello ");
        assertParameter(template.parameters, 3, "baz", "bar");
        assertParameter(template.parameters, 4, "4", " world");
    }

    @Test
    public void testInvalidTemplateNames() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{[invalid]}}");
        assertEquals(0, templates.size());
    }

    @Test
    public void testMagicTemplateName() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{#foo|bar}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("#foo", template.name);
        assertTrue(template.isMagic());
        assertEquals(1, template.parameters.size());
        assertEquals("{{#foo|bar}}", template.text);
    }

    @Test
    public void testTagTemplateName() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{#tag|span}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("#tag", template.name);
        assertTrue(template.isTag());
        assertEquals(1, template.parameters.size());
        assertEquals("{{#tag|span}}", template.text);
    }

    @Test
    public void testFormattingTemplateName() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{lc:this text will all be treated|as|lowercase}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("lc:this text will all be treated|as|lowercase", template.name);
        assertTrue(template.isFormatting());
        assertEquals(0, template.parameters.size());
        assertEquals("{{lc:this text will all be treated|as|lowercase}}", template.text);
    }

    @Test
    public void testTitleVariable() throws IOException {
        ArrayList<TemplateReference> templates = this.quickParse("{{FULLPAGENAME}}");
        assertEquals(1, templates.size());
        TemplateReference template = templates.get(0);
        assertEquals("FULLPAGENAME", template.name);
        assertTrue(template.isTitleVariable());
        assertEquals(0, template.parameters.size());
        assertEquals("{{FULLPAGENAME}}", template.text);
    }
}
