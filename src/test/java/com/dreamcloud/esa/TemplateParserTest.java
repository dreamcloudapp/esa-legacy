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
}
