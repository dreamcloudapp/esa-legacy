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
    protected static ArrayList<String> starts = null;

    public TemplateParser() {

    }

    protected static ArrayList<String> invalidTemplateStarts() {
        if (starts == null) {
            starts = new ArrayList<>();
            starts.add("__TOC__");
            starts.add("__FORCETOC__");
            starts.add("__NOTOC__");
            starts.add("__NOEDITSECTION__");
            starts.add("__NEWSECTIONLINK__");
            starts.add("__NONEWSECTIONLINK__");
            starts.add("__NOGALLERY__");
            starts.add("__HIDDENCAT__");
            starts.add("__INDEX__");
            starts.add("__NOINDEX__");
            starts.add("__STATICREDIRECT__");
            starts.add("__DISAMBIG__");
            starts.add("DISPLAYTITLE");
            starts.add("DEFAULTSORT");
            starts.add("NOEXTERNALLANGLINKS");
            starts.add("FULLPAGENAME");
            starts.add("PAGENAME");
            starts.add("BASEPAGENAME");
            starts.add("ROOTPAGENAME");
            starts.add("SUBPAGENAME");
            starts.add("ARTICLEPAGENAME");
            starts.add("SUBJECTPAGENAME");
            starts.add("TALKPAGENAME");
            starts.add("NAMESPACENUMBER");
            starts.add("NAMESPACE");
            starts.add("ARTICLESPACE");
            starts.add("SUBJECTSPACE");
            starts.add("TALKSPACE");
            starts.add("FULLPAGENAMEE");
            starts.add("PAGENAMEE");
            starts.add("BASEPAGENAMEE");
            starts.add("ROOTPAGENAMEE");
            starts.add("SUBPAGENAMEE");
            starts.add("ARTICLEPAGENAMEE");
            starts.add("SUBJECTPAGENAMEE");
            starts.add("TALKPAGENAMEE");
            starts.add("SHORTDESC");
            starts.add("SITENAME");
            starts.add("SERVER");
            starts.add("SERVERNAME");
            starts.add("SCRIPTPATH");
            starts.add("SITENAME");
            starts.add("CURRENTVERSION");
            starts.add("CURRENTYEAR");
            starts.add("CURRENTMONTH");
            starts.add("CURRENTMONTHNAME");
            starts.add("CURRENTMONTHABBREV");
            starts.add("CURRENTDAY");
            starts.add("CURRENTDAY2");
            starts.add("CURRENTDOW");
            starts.add("CURRENTDAYNAME");
            starts.add("CURRENTTIME");
            starts.add("CURRENTHOUR");
            starts.add("CURRENTWEEK");
            starts.add("CURRENTTIMESTAMP");
            starts.add("REVISIONDAY");
            starts.add("REVISIONDAY2");
            starts.add("REVISIONMONTH");
            starts.add("REVISIONYEAR");
            starts.add("REVISIONTIMESTAMP");
            starts.add("REVISIONUSER");
            starts.add("NUMBEROFPAGES");
            starts.add("NUMBEROFARTICLES");
            starts.add("NUMBEROFFILES");
            starts.add("NUMBEROFEDITS");
            starts.add("NUMBEROFUSERS");
            starts.add("NUMBEROFADMINS");
            starts.add("NUMBEROFACTIVEUSERS");
            starts.add("PAGEID");
            starts.add("PAGESIZE");
            starts.add("PROTECTIONLEVEL");
            starts.add("PROTECTIONEXPIRY");
            starts.add("PENDINGCHANGELEVEL");
            starts.add("PENDINGCHANGELEVEL");
            starts.add("PAGESINCATEGORY");
            starts.add("NUMBERINGROUP");
            starts.add("lc");
            starts.add("lcfirst");
            starts.add("uc");
            starts.add("ucfirst");
            starts.add("formatnum");
            starts.add("padleft");
            starts.add("padright");
            starts.add("plural");
            starts.add("gender");
            starts.add("localurl");
            starts.add("fullurl");
            starts.add("canonicalurl");
            starts.add("filepath");
            starts.add("urlencode");
            starts.add("anchorencode");
            starts.add("ns");
            starts.add("int");
        }
        return starts;
    }

    public boolean isTemplateStartValid(int c) {
        return c != '{' && c != '#'  && c != '<' && c != '>' && c != '[' && c !=']' && c != '}'  && c != '|';
    }

    public boolean isTemplateNameValid(String name) {
        if (name.length() == 0) {
            return false;
        }

        if (!isTemplateStartValid(name.charAt(0))) {
            return false;
        }

        for (String start: invalidTemplateStarts()) {
            if (name.startsWith(start)) {
                return false;
            }
        }

        return true;
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
                    if (isTemplateStartValid(peek)) {
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

        template.name = template.name.trim();
        if (!isTemplateNameValid(template.name)) {
            return null;
        }

        template.text = templateText.toString();
        return template;
    }
}
