package com.dreamcloud.esa.parser;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;

public class TemplateParser {
    ArrayList<TemplateReference> templateReferences;
    protected boolean inTemplate = false;
    protected boolean inParameter = false;
    protected int bracesSeen = 0;
    protected StringBuilder content;
    StringBuilder templateText;
    protected TemplateReference template = null;
    protected TemplateParameter parameter = null;

    public TemplateParser() {

    }

    public void reset() {
        templateReferences = new ArrayList<>();
        resetTemplate();
    }

    public void resetTemplate() {
        inTemplate = false;
        inParameter = false;
        bracesSeen = 0;
        content = new StringBuilder();
        templateText = new StringBuilder();
        template = new TemplateReference();
        parameter = new TemplateParameter();
    }

    public ArrayList<TemplateReference> parseTemplates(PushbackReader reader) throws IOException {
        reset();
        int c;

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
                                    template.addParameter(parameter);
                                } else {
                                    template.name = content.toString();
                                }
                            }
                            if (template.name != null) {
                                template.text = templateText.toString();
                                templateReferences.add(template);
                            }
                            resetTemplate();
                        }
                    }
                    break;
                case '|':
                    if (bracesSeen == 2) {
                        if (inParameter) {
                            //End of the parameter
                            parameter.value = content.toString();
                            template.addParameter(parameter);
                            content = new StringBuilder();

                            parameter = new TemplateParameter();
                        } else if(inTemplate) {
                            inParameter = true;
                            parameter = new TemplateParameter();
                            template.name = content.toString();
                            content = new StringBuilder();
                        }
                        break;
                    }
                case '=':
                    if (bracesSeen == 2) {
                        if (inParameter) {
                            parameter.name = content.toString();
                            content = new StringBuilder();
                        }
                        break;
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
