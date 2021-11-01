package com.dreamcloud.esa.parser;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;

public class TemplateParser {
    ArrayList<TemplateReference> templateReferences;
    protected boolean inTemplate = false;
    protected boolean inParameter = false;
    protected int bracesSeen = 0;
    protected StringBuilder content;
    StringBuilder templateText;

    public TemplateParser() {

    }

    public void reset() {
        inTemplate = false;
        inParameter = false;
        bracesSeen = 0;
        content = new StringBuilder();
        templateText = new StringBuilder();
        templateReferences = new ArrayList<>();
    }

    public void resetTemplate() {
        inTemplate = false;
        inParameter = false;
        bracesSeen = 0;
        content = new StringBuilder();
        templateText = new StringBuilder();
    }

    public ArrayList<TemplateReference> parseTemplates(PushbackReader reader) throws IOException {
        reset();
        int c;
        TemplateReference template = null;
        TemplateParameter parameter = null;

        while ((c = reader.read()) != -1) {
            if (inTemplate) {
                //Keep track of the exact template text
                templateText.append((char) c);
            }
            //Basic character handling
            switch (c) {
                case '{':
                    bracesSeen++;
                    int peek = reader.read();
                    reader.unread(peek);
                    if (bracesSeen == 2 && peek != '{') {
                        inTemplate = true;
                        template = new TemplateReference();
                        templateText = new StringBuilder();
                        templateText.append("{{");
                    }
                    break;
                case '}':
                    if (inTemplate) {
                        if (--bracesSeen == 0) {
                            if (content.length() > 0) {
                                if (inParameter) {
                                    parameter.value = content.toString();
                                } else {
                                    template.name = content.toString();
                                }
                            }
                            templateReferences.add(template);
                            resetTemplate();
                        }
                    }
                    break;
                case '|':
                    if (inParameter) {
                        //End of the parameter
                        parameter.value = content.toString();
                        content = new StringBuilder();
                        template.parameters.add(parameter);
                    } else if(inTemplate) {
                        inParameter = true;
                        parameter = new TemplateParameter();
                        template.name = content.toString();
                        content = new StringBuilder();
                    }
                    break;
                case '=':
                    if (inParameter) {
                        parameter.name = content.toString();
                        content = new StringBuilder();
                    }
                default:
                    if (!inTemplate) {
                        bracesSeen = 0;
                    } else {
                        content.append((char) c);
                        //If we ever find "nowiki" in here, then it's not a real template
                        if (content.indexOf("nowiki") > 0) {
                            resetTemplate();
                        }
                    }
            }
        }
        return templateReferences;
    }
}
