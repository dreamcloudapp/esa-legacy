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
    private static Pattern variablePattern = Pattern.compile("\\{\\{\\{([^|}]+)(\\|[^}]*)?}}}");
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

    public String substitute(String text, String title, ArrayList<String> templatesSeen, int depth) throws IOException {
        if (depth > options.recursionDepth) {
            return text;
        }

        if (depth == 1) {
            processed++;
        }

        TemplateParser parser = new TemplateParser();
        ArrayList<TemplateReference> templateReferences = parser.parse(
                new PushbackReader(new StringReader(text))
        );
        templateReferenceCount += templateReferences.size();

        int maxTemplates = depth > 1 ? options.maxTemplates : 0;
        int templateCount = 0;
        for (TemplateReference templateReference: templateReferences) {
            boolean templateMaxExceeded = maxTemplates > 0 && templateCount > maxTemplates;

            String templateName = StringUtils.normalizeWikiTitle(templateReference.name);
            boolean templateExists = templateMap.containsKey(templateName);
            if (templateReference.isTitleVariable()) {
                text = text.replace(templateReference.text, title);
            } else if(templateReference.isFormatting() || templateReference.isTag()) {
                char split = templateReference.isFormatting() ? ':' : '|';
                int splitPos = templateReference.name.indexOf(split);
                if (splitPos == -1 || templateReference.name.length() == splitPos + 1) {
                    text = text.replace(templateReference.text, "");
                } else {
                    text = text.replace(templateReference.text, templateReference.name.substring(splitPos + 1));
                }
            } else if(templateReference.isMagic()) {
                text = text.replace(templateReference.text, "");
            } else if (templateExists && !templatesSeen.contains(templateName) && !templateMaxExceeded) {
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
                    if (variableMatcher.group(2) != null) {
                        variable.defaultValue = variableMatcher.group(2).substring(1);
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
                        templateText = templateText.replace(variable.text, replacement);
                    }
                }

                templatesSeen.add(templateName);
                templateText = substitute(templateText, title, templatesSeen, depth + 1);
                templatesSeen.remove(templateName);
                text = text.replaceFirst(Pattern.quote(templateReference.text), Matcher.quoteReplacement(templateText));
                templateCount++;
            } else {
                StringBuilder replacement = new StringBuilder();
                for (TemplateParameter parameter: templateReference.parameters) {
                    if (parameter.value != null) {
                        replacement.append(parameter.value).append(' ');
                    }
                }
                text = text.replace(templateReference.text, replacement.toString());
            }
        }
        return text;
    }

    public String substitute(String text, String title, ArrayList<String> templatesSeen) throws IOException {
        return substitute(text, title, templatesSeen, 1);
    }

    public String substitute(String text, String title) throws IOException {
        ArrayList<String> templatesSeen = new ArrayList<>();
        return substitute(text, title, templatesSeen);
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
