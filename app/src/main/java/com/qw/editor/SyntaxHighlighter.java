package com.qw.editor;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

import androidx.core.content.ContextCompat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxHighlighter {

    public enum Lang { PYTHON, HTML, CSS, JS, PHP, JAVA, JSON, XML, MARKDOWN, C, PLAIN }

    private static final String[] PY_KEYWORDS = {
            "def","class","return","if","elif","else","for","while","import","from","as",
            "try","except","finally","with","lambda","pass","break","continue","in","is",
            "not","and","or","None","True","False","self","yield","raise","global","print"
    };

    private static final String[] JS_KEYWORDS = {
            "function","return","if","else","for","while","var","let","const","new","this",
            "class","extends","try","catch","finally","switch","case","break","continue",
            "true","false","null","undefined","import","export","default","async","await","typeof"
    };

    private static final String[] PHP_KEYWORDS = {
            "function","return","if","else","elseif","foreach","for","while","echo","class",
            "extends","public","private","protected","static","new","try","catch","finally",
            "namespace","use","require","include","true","false","null","array","print","switch","case"
    };

    private static final String[] JAVA_KEYWORDS = {
            "public","private","protected","static","final","class","interface","extends",
            "implements","return","if","else","for","while","do","switch","case","break",
            "continue","new","try","catch","finally","throw","throws","import","package",
            "void","int","long","float","double","boolean","char","byte","short","this",
            "super","null","true","false","enum","abstract","synchronized","volatile"
    };

    private static final String[] C_KEYWORDS = {
            "int","float","double","char","void","short","long","unsigned","signed","struct",
            "union","enum","typedef","static","const","return","if","else","for","while","do",
            "switch","case","break","continue","sizeof","include","define","ifdef","ifndef",
            "endif","null","true","false"
    };

    public static Lang detectLang(String fileName) {
        String n = fileName.toLowerCase(Locale.ROOT);
        if (n.endsWith(".py")) return Lang.PYTHON;
        if (n.endsWith(".html") || n.endsWith(".htm")) return Lang.HTML;
        if (n.endsWith(".css")) return Lang.CSS;
        if (n.endsWith(".js") || n.endsWith(".ts") || n.endsWith(".jsx")) return Lang.JS;
        if (n.endsWith(".php")) return Lang.PHP;
        if (n.endsWith(".java") || n.endsWith(".kt")) return Lang.JAVA;
        if (n.endsWith(".json")) return Lang.JSON;
        if (n.endsWith(".xml")) return Lang.XML;
        if (n.endsWith(".md") || n.endsWith(".markdown")) return Lang.MARKDOWN;
        if (n.endsWith(".c") || n.endsWith(".h") || n.endsWith(".cpp") || n.endsWith(".cc")) return Lang.C;
        return Lang.PLAIN;
    }

    public static void highlight(Context ctx, Editable text, Lang lang) {
        // إزالة التلوين السابق
        ForegroundColorSpan[] spans = text.getSpans(0, text.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            text.removeSpan(span);
        }
        if (lang == Lang.PLAIN) return;

        int keyword = ContextCompat.getColor(ctx, R.color.qw_keyword);
        int string = ContextCompat.getColor(ctx, R.color.qw_string);
        int comment = ContextCompat.getColor(ctx, R.color.qw_comment);
        int number = ContextCompat.getColor(ctx, R.color.qw_number);
        int tag = ContextCompat.getColor(ctx, R.color.qw_tag);

        switch (lang) {
            case PYTHON:
                applyKeywords(text, PY_KEYWORDS, keyword);
                applyPattern(text, "#.*", comment);
                applyPattern(text, "(\"[^\"]*\"|'[^']*')", string);
                applyPattern(text, "\\b\\d+(\\.\\d+)?\\b", number);
                break;
            case JS:
                applyKeywords(text, JS_KEYWORDS, keyword);
                applyPattern(text, "//.*", comment);
                applyPattern(text, "/\\*[\\s\\S]*?\\*/", comment);
                applyPattern(text, "(\"[^\"]*\"|'[^']*'|`[^`]*`)", string);
                applyPattern(text, "\\b\\d+(\\.\\d+)?\\b", number);
                break;
            case PHP:
                applyKeywords(text, PHP_KEYWORDS, keyword);
                applyPattern(text, "//.*", comment);
                applyPattern(text, "#.*", comment);
                applyPattern(text, "/\\*[\\s\\S]*?\\*/", comment);
                applyPattern(text, "(\"[^\"]*\"|'[^']*')", string);
                applyPattern(text, "\\$\\w+", tag);
                applyPattern(text, "\\b\\d+(\\.\\d+)?\\b", number);
                break;
            case JAVA:
                applyKeywords(text, JAVA_KEYWORDS, keyword);
                applyPattern(text, "//.*", comment);
                applyPattern(text, "/\\*[\\s\\S]*?\\*/", comment);
                applyPattern(text, "(\"[^\"]*\"|'[^']*')", string);
                applyPattern(text, "@\\w+", tag);
                applyPattern(text, "\\b\\d+(\\.\\d+)?\\b", number);
                break;
            case C:
                applyKeywords(text, C_KEYWORDS, keyword);
                applyPattern(text, "//.*", comment);
                applyPattern(text, "/\\*[\\s\\S]*?\\*/", comment);
                applyPattern(text, "(\"[^\"]*\"|'[^']*')", string);
                applyPattern(text, "#\\s*\\w+", tag);
                applyPattern(text, "\\b\\d+(\\.\\d+)?\\b", number);
                break;
            case HTML:
            case XML:
                applyPattern(text, "</?[a-zA-Z0-9:_-]+", tag);
                applyPattern(text, "[a-zA-Z-]+(?==)", keyword);
                applyPattern(text, "(\"[^\"]*\"|'[^']*')", string);
                applyPattern(text, "<!--[\\s\\S]*?-->", comment);
                break;
            case CSS:
                applyPattern(text, "[.#]?[a-zA-Z0-9_-]+(?=\\s*\\{)", tag);
                applyPattern(text, "[a-zA-Z-]+(?=\\s*:)", keyword);
                applyPattern(text, "(\"[^\"]*\"|'[^']*')", string);
                applyPattern(text, "/\\*[\\s\\S]*?\\*/", comment);
                applyPattern(text, "#[0-9a-fA-F]{3,6}\\b", number);
                break;
            case JSON:
                applyPattern(text, "\"[^\"]*\"\\s*(?=:)", tag);
                applyPattern(text, ":\\s*(\"[^\"]*\")", string);
                applyPattern(text, "\\b(true|false|null)\\b", keyword);
                applyPattern(text, "\\b\\d+(\\.\\d+)?\\b", number);
                break;
            case MARKDOWN:
                applyPattern(text, "^#{1,6}\\s.*$", keyword);
                applyPattern(text, "\\*\\*[^*]+\\*\\*", tag);
                applyPattern(text, "`[^`]*`", string);
                applyPattern(text, "^>.*$", comment);
                break;
        }
    }

    private static void applyKeywords(Editable text, String[] keywords, int color) {
        StringBuilder pattern = new StringBuilder("\\b(");
        for (int i = 0; i < keywords.length; i++) {
            pattern.append(Pattern.quote(keywords[i]));
            if (i < keywords.length - 1) pattern.append("|");
        }
        pattern.append(")\\b");
        applyPattern(text, pattern.toString(), color);
    }

    private static void applyPattern(Editable text, String regex, int color) {
        try {
            Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = p.matcher(text);
            while (m.find()) {
                text.setSpan(new ForegroundColorSpan(color), m.start(), m.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } catch (Exception ignored) {
        }
    }
}
