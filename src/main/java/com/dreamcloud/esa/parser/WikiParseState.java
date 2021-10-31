package com.dreamcloud.esa.parser;

import java.io.IOException;

interface WikiParseState {
    public void handleCharacter(int c);
    public boolean isAccepting();
    public WikiParseToken resolve(TemplateParser parser) throws IOException;
}
