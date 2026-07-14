package com.qw.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class CodeAutoFormatter implements TextWatcher {

    private final EditText editText;
    private int start, before, count;
    private boolean editing = false;
    private String indentUnit = "    ";

    public CodeAutoFormatter(EditText editText) {
        this.editText = editText;
    }

    public void setTabSize(int spaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) sb.append(' ');
        this.indentUnit = sb.toString();
    }

    public String getIndentUnit() {
        return indentUnit;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        this.start = start;
        this.before = before;
        this.count = count;
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (editing) return;
        if (count == 1 && before == 0 && start >= 0 && start < s.length()) {
            char c = s.charAt(start);
            editing = true;
            try {
                if (c == '\n') {
                    handleNewline(s, start);
                } else {
                    handleAutoClose(s, start, c);
                }
            } finally {
                editing = false;
            }
        }
    }

    private void handleNewline(Editable s, int pos) {
        int idx = pos - 1;
        int lineStart = idx;
        while (lineStart > 0 && s.charAt(lineStart - 1) != '\n') lineStart--;
        if (idx < lineStart) return;
        String prevLine = s.subSequence(lineStart, idx + 1).toString();

        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < prevLine.length(); i++) {
            char ch = prevLine.charAt(i);
            if (ch == ' ' || ch == '\t') indent.append(ch);
            else break;
        }

        String trimmed = prevLine.trim();
        if (trimmed.endsWith("{") || trimmed.endsWith(":")) {
            indent.append(indentUnit);
        }

        if (indent.length() > 0) {
            s.insert(pos + 1, indent.toString());
            editText.setSelection(pos + 1 + indent.length());
        }
    }

    private void handleAutoClose(Editable s, int pos, char c) {
        char close = 0;
        switch (c) {
            case '(': close = ')'; break;
            case '{': close = '}'; break;
            case '[': close = ']'; break;
            case '"': close = '"'; break;
            case '\'': close = '\''; break;
        }
        if (close != 0) {
            // تجنب التكرار إذا كان الحرف التالي مطابقًا فعلاً (حالة الاقتباسات المزدوجة)
            boolean alreadyClosed = pos + 1 < s.length() && s.charAt(pos + 1) == close
                    && (c == '"' || c == '\'');
            if (!alreadyClosed) {
                s.insert(pos + 1, String.valueOf(close));
            }
            editText.setSelection(pos + 1);
        }
    }
}
