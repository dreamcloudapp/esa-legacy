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

    protected int templateReferenceCount = 0;
    protected int variableCount = 0;
    protected int variableReplacements = 0;
    protected int defaultVariableReplacements = 0;

    public TemplateProcessor(Map<String, String> templateMap, TemplateResolutionOptions options) {
        this.templateMap = templateMap;
        this.options = options;
    }

    public String substitute(String text, int depth) throws IOException {
        if (depth > options.recursionDepth) {
            return text;
        }

        TemplateParser parser = new TemplateParser();
        ArrayList<TemplateReference> templateReferences = parser.parseTemplates(
                new PushbackReader(new StringReader(text))
        );
        templateReferenceCount += templateReferences.size();

        for (TemplateReference templateReference: templateReferences) {
            String templateName = StringUtils.normalizeWikiTitle(templateReference.name);
            if (templateMap.containsKey(templateName)) {
                Map<String, TemplateParameter> templateParameterMap = new HashMap<>();
                int parameterCount = 0;
                for (TemplateParameter parameter: templateReference.parameters) {
                    String parameterName = parameter.name != null ? parameter.name : String.valueOf(parameterCount++);
                    templateParameterMap.put(parameterName, parameter);
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
                        templateText = templateText.replaceAll(variable.text, replacement);
                    }
                }

                templateText = substitute(templateText, depth + 1);
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

    public String substitute(String text) throws IOException {
        return substitute(text, 0);
    }

    public void displayInfo() {
        System.out.println("Template Info:");
        System.out.println("----------------------------------------");
        System.out.println("Templates Refs:\t" + templateReferenceCount);
        System.out.println("Variable Refs:\t" + variableCount);
        System.out.println("Variables Replaced:\t" + variableReplacements);
        System.out.println("Defaulted Variables:\t" + defaultVariableReplacements);
        System.out.println("----------------------------------------");
    }
}
