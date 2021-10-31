package com.dreamcloud.esa.annoatation;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

interface TitleCondition {
    boolean matches(String title);
}

class RegexCondition implements TitleCondition {
    protected Pattern pattern;

    public RegexCondition(Pattern pattern) {
        this.pattern = pattern;
    }

    public boolean matches(String title) {
        Matcher matcher = pattern.matcher(title);
        return matcher.find();
    }
}

class StartsWithCondition implements TitleCondition {
    protected String start;

    public StartsWithCondition(String start) {
        this.start = start;
    }

    public boolean matches(String title) {
        return title.startsWith(start);
    }
}

class ContainsCondition implements TitleCondition {
    protected String substring;

    public ContainsCondition(String substring) {
        this.substring = substring;
    }

    public boolean matches(String title) {
        return title.contains(substring);
    }
}

/**
 * Takes regex, contains, and starts with conditions and checks
 * Wikipedia titles against them
 */
public class WikiTitleMatcher {
    ArrayList<TitleCondition> conditions;

    public WikiTitleMatcher() {
        conditions = new ArrayList<>();
    }

    void addRegexCondition(String regex) {
        this.addRegexCondition(Pattern.compile(regex));
    }

    void addRegexCondition(Pattern pattern) {
        this.conditions.add(new RegexCondition(pattern));
    }

    void addStartsWithCondition(String start) {
        this.conditions.add(new StartsWithCondition(start));
    }

    void addContainsCondition(String substring) {
        this.conditions.add(new ContainsCondition(substring));
    }

    boolean matches(String title) {
        for (TitleCondition condition: conditions) {
            if (condition.matches(title)) {
                return true;
            }
        }
        return false;
    }

    public static WikiTitleMatcher createForTemplateStripping() {
        WikiTitleMatcher matcher = new WikiTitleMatcher();
        matcher.addRegexCondition(
            Pattern.compile("stub|user[ s]|disambig|copy-?edit|cleanup|page|article|/meta|/doc|category|documentation|cite|css|see below|see at|aka|see above|image|/format|wiki|disputed|please|wrongtitle|color([^a]|$)|font|lang-|time zone|icon|todo|style/|unit of|^template:[.]{1,3}$|warn-|^template:.*template.*$|discussion|usertalk|column|col-| age |did you know|uw-|see also|this is|don't|word|split|styles|^template:[A-Z]+$|citation|str ")
        );
        return matcher;
    }
}
