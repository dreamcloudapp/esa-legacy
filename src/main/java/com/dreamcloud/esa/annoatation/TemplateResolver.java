package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.annoatation.handler.XmlWritingHandler;
import com.dreamcloud.esa.parser.TemplateParameter;
import com.dreamcloud.esa.parser.TemplateParser;
import com.dreamcloud.esa.parser.TemplateReference;
import com.dreamcloud.esa.tools.BZipFileReader;
import com.dreamcloud.esa.tools.StringUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;

public class TemplateResolver extends XmlWritingHandler {
    TemplateResolutionOptions options;
    Map<String, String> templateMap;
    protected final SAXParserFactory saxFactory;
    protected int templates = 0;

    public TemplateResolver(TemplateResolutionOptions options, Map<String, String> templateMap) {
        this.options = options;
        this.templateMap = templateMap;
        this.setDocumentTag("page");
        saxFactory = SAXParserFactory.newInstance();
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(false);
        saxFactory.setXIncludeAware(true);
    }

    public void resolve(File inputFile, File outputFile) throws IOException, ParserConfigurationException, SAXException, XMLStreamException {
        reset();
        SAXParser saxParser = saxFactory.newSAXParser();
        Reader reader = BZipFileReader.getFileReader(inputFile);
        InputSource is = new InputSource(reader);
        is.setEncoding("UTF-8");

        //Begin the XML document
        this.open(outputFile);
        this.writeDocumentBegin("doc");

        saxParser.parse(is, this);
        reader.close();

        //End document
        this.writeDocumentEnd();

        //Show logs
        System.out.println("----------------------------------------");
        System.out.println("Articles Read:\t" + this.getDocsRead());
        System.out.println("Templates Refs:\t" + templates);
        System.out.println("----------------------------------------");
    }

    public void handleDocument(Map<String, String> xmlFields) {
        int docsRead = this.getDocsRead();
        if (docsRead % 1000 == 0) {
            System.out.println("processed template\t[" + templates + " | " + docsRead + "]");
        }
        String title = xmlFields.get("title");

        //We aren't going to write templates
        if (title.startsWith("Template:")) {
            return;
        }

        String text = xmlFields.get("text");

        try {
            text = this.resolveTemplates(text, templateMap, 0);
            this.writeDocument(StringUtils.normalizeWikiTitle(title), text);
        } catch (XMLStreamException | IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String resolveTemplates(String text, Map<String, String> templateMap, int depth) throws IOException {
        if (depth > options.recursionDepth) {
            return text;
        }

        TemplateParser parser = new TemplateParser();
        ArrayList<TemplateReference> templateReferences = parser.parseTemplates(
            new StringReader(text)
        );
        templates += templateReferences.size();

        for (TemplateReference templateReference: templateReferences) {
            String templateName = StringUtils.normalizeWikiTitle(templateReference.name);
            if (templateMap.containsKey(templateName)) {
                String templateText = templateMap.get(templateName);

                //Replace the parameters
                int parameterCount = 1;
                for (TemplateParameter parameter: templateReference.parameters) {
                    if (!"".equals(parameter.name)) {
                        templateText = templateText.replaceAll("\\{{\\{\\{" + parameter.name + "}}}", Matcher.quoteReplacement(parameter.value));
                    } else {
                        templateText = templateText.replaceAll("\\{\\{\\{" + parameterCount + "}}}", Matcher.quoteReplacement(parameter.value));
                    }
                    parameterCount++;
                }

                templateText = resolveTemplates(templateText, templateMap, depth + 1);
                text = text.replaceFirst(templateReference.text, Matcher.quoteReplacement(templateText));
            } else {
                StringBuilder replacement = new StringBuilder();
                for (TemplateParameter parameter: templateReference.parameters) {
                    replacement.append(parameter.name).append(' ').append(parameter.value);
                }
                text = text.replaceFirst(templateReference.text, Matcher.quoteReplacement(replacement.toString()));
            }
        }

        return text;
    }

    public void writeDocument(String title, String text) throws XMLStreamException, IOException {
        this.writeStartElement("doc");
        this.writeElement("title", title);
        this.writeElement("text", text);
        this.writeEndElement();
    }
}
