package com.dreamcloud.esa.parser;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;

/**
 * A simple parser to extract templates from Wikipedia text.
 *
 * @note The parse method is thread safe.
 * @note The parser doesn't handle nested templates
 */
public class TemplateParser {
    public TemplateParser() {

    }

    public ArrayList<TemplateReference> parse(PushbackReader reader) throws IOException {
        ArrayList<TemplateReference> templateReferences = new ArrayList<>();
        int bracesSeen = 0;
        int c;
        while ((c = reader.read()) != -1) {
            if (c == '{') {
                if (++bracesSeen == 2) {
                    int peek = reader.read();
                    reader.unread(peek);
                    if (peek != '{') {
                        TemplateReference template = parseTemplate(reader);
                        if (template != null) {
                            templateReferences.add(template);
                        }
                    }
                    bracesSeen = 0;
                }
            } else {
                bracesSeen = 0;
            }
        }
        return templateReferences;
    }

    protected TemplateReference parseTemplate(PushbackReader reader) throws IOException {
        TemplateReference template = new TemplateReference();
        int depth = 2;
        StringBuilder templateText = new StringBuilder("{{");
        StringBuilder content = new StringBuilder();
        TemplateParameter parameter = null;

        while (depth > 0) {
            int c = reader.read();
            if (c == -1) {
                return null;
            }
            templateText.append((char) c);

            switch (c) {
                case '{':
                case '[':
                    depth++;
                    content.append((char) c);
                    break;
                case ']':
                    depth--;
                    content.append((char) c);
                    break;
                case '}':
                    if (depth > 2) {
                        content.append((char) c);
                    }
                    depth--;
                    break;
                default:
                    if (depth == 2) {
                        switch (c) {
                            case '|':
                                if (parameter != null) {
                                    parameter.value = content.toString();
                                    template.addParameter(parameter);
                                } else {
                                    template.name = content.toString();
                                }
                                parameter = new TemplateParameter();
                                content = new StringBuilder();
                                break;
                            case '=':
                                if (parameter != null) {
                                    parameter.name = content.toString();
                                    content = new StringBuilder();
                                }
                                break;
                            default:
                                content.append((char) c);
                        }
                    } else {
                        content.append((char) c);
                    }
            }
        }
        if (content.length() > 0) {
            if (parameter != null) {
                parameter.value = content.toString();
                template.addParameter(parameter);
            } else {
                template.name = content.toString();
            }
        }
        template.text = templateText.toString();
        return template;
    }
}
