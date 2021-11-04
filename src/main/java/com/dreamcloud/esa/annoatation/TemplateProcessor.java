package com.dreamcloud.esa.annoatation;

import com.dreamcloud.esa.parser.TemplateParameter;
import com.dreamcloud.esa.parser.TemplateParser;
import com.dreamcloud.esa.parser.TemplateReference;
import com.dreamcloud.esa.tools.StringUtils;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateProcessor {
    private static Pattern variablePattern = Pattern.compile("\\{\\{\\{([^|}]+)\\|?([^}]*)}}}");
    protected Map<String, String> templateMap;
    protected TemplateResolutionOptions options;

    protected int processed = 0;
    protected int templateReferenceCount = 0;
    protected int variableCount = 0;
    protected int variableReplacements = 0;
    protected int defaultVariableReplacements = 0;
    protected int nuked = 0;

    public TemplateProcessor(Map<String, String> templateMap, TemplateResolutionOptions options) {
        this.templateMap = templateMap;
        this.options = options;
    }

    public String substitute(String text, ArrayList<String> templatesSeen, int depth) throws IOException {
        if (depth > options.recursionDepth) {
            return text;
        }

        if (depth == 0) {
            processed++;
        }

        System.out.println("processed (" + depth + "):\t" + processed);

        TemplateParser parser = new TemplateParser();
        ArrayList<TemplateReference> templateReferences = parser.parse(
                new PushbackReader(new StringReader(text))
        );
        templateReferenceCount += templateReferences.size();

        System.out.println("template ref count: " + templateReferences.size());

        for (TemplateReference templateReference: templateReferences) {
            String templateName = StringUtils.normalizeWikiTitle(templateReference.name);
            boolean templateExists = templateMap.containsKey(templateName);
            if (templateExists && !templatesSeen.contains(templateName)) {
                Map<String, TemplateParameter> templateParameterMap = new HashMap<>();
                int parameterCount = 0;
                for (TemplateParameter parameter: templateReference.parameters) {
                    String parameterName = parameter.name != null ? parameter.name : String.valueOf(parameterCount);
                    templateParameterMap.put(parameterName, parameter);
                    parameterCount++;
                }

                String templateText = templateMap.get(templateName);

                //Find the variables used by the template
                Matcher variableMatcher = variablePattern.matcher(templateText);
                ArrayList<WikiVariable> variables = new ArrayList<>();
                while (variableMatcher.find()) {
                    variableCount++;
                    WikiVariable variable = new WikiVariable();
                    variable.text = variableMatcher.group();
                    variable.name = variableMatcher.group(1);
                    if (variableMatcher.groupCount() > 1) {
                        variable.defaultValue = variableMatcher.group(2);
                    }
                    variables.add(variable);
                }

                //Replace the variables with parameters
                for (WikiVariable variable: variables) {
                    TemplateParameter parameter = templateParameterMap.get(variable.name);
                    String replacement = null;
                    if (parameter != null) {
                        replacement = parameter.value;
                    } else if (variable.defaultValue != null) {
                        replacement = variable.defaultValue;
                        defaultVariableReplacements++;
                    }
                    if (replacement != null) {
                        variableReplacements++;
                        if (replacement.contains(variable.text)) {
                            System.out.println("recursive replace: " + replacement + " : " + variable.text);
                            System.exit(1);
                        }
                        templateText = templateText.replace(variable.text, replacement);
                    }
                }

                templatesSeen.add(templateName);
                templateText = substitute(templateText, templatesSeen, depth + 1);
                templatesSeen.remove(templateName);
                text = text.replaceFirst(Pattern.quote(templateReference.text), Matcher.quoteReplacement(templateText));
            } else if(templateReference.name.startsWith("#")) {
                //Nuke 'em!
                nuked++;
                text = text.replace(templateReference.text, "");
            }
            else {
                StringBuilder replacement = new StringBuilder();
                for (TemplateParameter parameter: templateReference.parameters) {
                    if (parameter.name !=  null) {
                        replacement.append(parameter.name).append(' ');
                    }
                    if (parameter.value != null) {
                        replacement.append(parameter.value).append(' ');
                    }
                }
                text = text.replace(templateReference.text, replacement.toString());
            }
        }

        return text;
    }

    public String substitute(String text, ArrayList<String> templatesSeen) throws IOException {
        return substitute(text, templatesSeen, 0);
    }

    public String substitute(String text) throws IOException {
        ArrayList<String> templatesSeen = new ArrayList<>();
        return substitute(text, templatesSeen);
    }

    public void displayInfo() {
        System.out.println("Template Info:");
        System.out.println("----------------------------------------");
        System.out.println("Templates Refs:\t" + templateReferenceCount);
        System.out.println("Variable Refs:\t" + variableCount);
        System.out.println("Variables Replaced:\t" + variableReplacements);
        System.out.println("Defaulted Variables:\t" + defaultVariableReplacements);
        System.out.println("Nuked Refs:\t" + nuked);
        System.out.println("----------------------------------------");
    }
}
