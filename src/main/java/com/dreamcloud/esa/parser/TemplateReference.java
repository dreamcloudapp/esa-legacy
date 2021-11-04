package com.dreamcloud.esa.parser;

import java.util.ArrayList;

public class TemplateReference {
    public String name = null;
    public ArrayList<TemplateParameter> parameters = new ArrayList<>();
    public String text = null;

    public void addParameter(TemplateParameter parameter) {
        int index = this.parameters.size() + 1;
        parameter.index = index;
        if (parameter.name == null) {
            parameter.name = String.valueOf(index);
        }
        this.parameters.add(parameter);
    }
}
