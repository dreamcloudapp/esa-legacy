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
        } else {
            //Trim names and values of named parameters per Wiki docs
            parameter.name = parameter.name.trim();
            parameter.value = parameter.value.trim();
        }
        this.parameters.add(parameter);
    }

    public boolean isTitleVariable() {
        return (name.startsWith("FULLPAGENAME") || name.startsWith("PAGENAME") || name.startsWith("BASEPAGENAME") || name.startsWith("ROOTPAGENAME"));
    }

    public boolean isTag() {
        return name.startsWith("#tag");
    }

    public boolean isFormatting() {
        return (name.startsWith("lc:") || name.startsWith("uc:") || name.startsWith("lcfirst:") || name.startsWith(("ucfirst:")));
    }

    public boolean isMagic() {
        return this.name.charAt(0) == '#';
    }
}
