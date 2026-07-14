package com.qw.editor;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public class EditorActivity extends BaseActivity {

    private EditText codeEditor;
    private TextView lineNumbers;
    private ScrollView verticalScroll;
    private HorizontalScrollView hScrollEditor;
    private View findBar;
    private EditText etSearch, etReplace;
    private TextView matchCount;
    private Toolbar toolbar;

    private File currentFile;
    private SyntaxHighlighter.Lang lang;
    private CodeAutoFormatter autoFormatter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable highlightRunnable;
    private Runnable undoSnapshotRunnable;
    private boolean isProgrammaticChange = false;

    private final Deque<String> undoStack = new ArrayDeque<>();
    private final Deque<String> redoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 50;

    private float fontSize = 14f;
    private boolean wordWrap = false;
    private boolean dirty = false;
    private String lastSavedContent = "";

    private final List<Integer> matchPositions = new ArrayList<>();
    private int currentMatch = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        String path = getIntent().getStringExtra("file_path");
        currentFile = new File(path);
        lang = SyntaxHighlighter.detectLang(currentFile.getName());

        toolbar = findViewById(R.id.editorToolbar);
        toolbar.setTitle(currentFile.getName());
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        codeEditor = findViewById(R.id.codeEditor);
        lineNumbers = findViewById(R.id.lineNumbers);
        verticalScroll = findViewById(R.id.verticalScroll);
        hScrollEditor = findViewById(R.id.hScrollEditor);
        findBar = findViewById(R.id.findBar);
        etSearch = findViewById(R.id.etSearch);
        etReplace = findViewById(R.id.etReplace);
        matchCount = findViewById(R.id.matchCount);

        fontSize = AppPreferences.getFontSize(this);
        wordWrap = AppPreferences.isWordWrapEnabled(this);
        codeEditor.setTextSize(fontSize);
        lineNumbers.setTextSize(fontSize);

        loadFile();
        setupToolButtons();
        setupTextWatcher();
        setupFindBar();
        applyWordWrap();
    }

    // ---------- تحميل وحفظ الملف ----------

    private void loadFile() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(currentFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (Exception ignored) {
        }
        String content = sb.toString();
        codeEditor.setText(content);
        SyntaxHighlighter.highlight(this, codeEditor.getText(), lang);
        updateLineNumbers();
        undoStack.push(content);
        lastSavedContent = content;
        setDirty(false);
    }

    private void saveFile() {
        try (FileOutputStream fos = new FileOutputStream(currentFile)) {
            String content = codeEditor.getText().toString();
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            lastSavedContent = content;
            setDirty(false);
            Toast.makeText(this, R.string.toast_saved, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.toast_save_failed, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFileSilently() {
        try (FileOutputStream fos = new FileOutputStream(currentFile)) {
            String content = codeEditor.getText().toString();
            fos.write(content.getBytes(StandardCharsets.UTF_8));
            lastSavedContent = content;
            setDirty(false);
        } catch (Exception ignored) {
        }
    }

    private void setDirty(boolean value) {
        dirty = value;
        String title = currentFile.getName() + (dirty ? " " + getString(R.string.unsaved_marker) : "");
        toolbar.setTitle(title);
    }

    // ---------- مراقبة النص: تلوين + ترقيم + تراجع ----------

    private void setupTextWatcher() {
        autoFormatter = new CodeAutoFormatter(codeEditor);
        autoFormatter.setTabSize(AppPreferences.getTabSize(this));
        codeEditor.addTextChangedListener(autoFormatter);

        codeEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateLineNumbers();

                if (highlightRunnable != null) handler.removeCallbacks(highlightRunnable);
                highlightRunnable = () -> SyntaxHighlighter.highlight(EditorActivity.this, s, lang);
                handler.postDelayed(highlightRunnable, 300);

                if (!isProgrammaticChange) {
                    setDirty(!s.toString().equals(lastSavedContent));

                    if (undoSnapshotRunnable != null) handler.removeCallbacks(undoSnapshotRunnable);
                    final String snapshot = s.toString();
                    undoSnapshotRunnable = () -> pushUndoSnapshot(snapshot);
                    handler.postDelayed(undoSnapshotRunnable, 600);
                }
            }
        });
    }

    private void pushUndoSnapshot(String snapshot) {
        if (!undoStack.isEmpty() && undoStack.peek().equals(snapshot)) return;
        undoStack.push(snapshot);
        redoStack.clear();
        while (undoStack.size() > MAX_UNDO) {
            undoStack.removeLast();
        }
    }

    private void performUndo() {
        if (undoStack.size() <= 1) {
            Toast.makeText(this, R.string.toast_no_more_undo, Toast.LENGTH_SHORT).show();
            return;
        }
        String current = undoStack.pop();
        redoStack.push(current);
        String previous = undoStack.peek();
        applyProgrammaticText(previous);
    }

    private void performRedo() {
        if (redoStack.isEmpty()) {
            Toast.makeText(this, R.string.toast_no_more_redo, Toast.LENGTH_SHORT).show();
            return;
        }
        String next = redoStack.pop();
        undoStack.push(next);
        applyProgrammaticText(next);
    }

    private void applyProgrammaticText(String content) {
        isProgrammaticChange = true;
        int cursor = Math.min(codeEditor.getSelectionStart(), content.length());
        codeEditor.setText(content);
        codeEditor.setSelection(Math.max(0, Math.min(cursor, content.length())));
        isProgrammaticChange = false;
        SyntaxHighlighter.highlight(this, codeEditor.getText(), lang);
        updateLineNumbers();
        setDirty(!content.equals(lastSavedContent));
    }

    private void updateLineNumbers() {
        int lines = codeEditor.getLineCount();
        if (lines < 1) lines = 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= lines; i++) {
            sb.append(i);
            if (i != lines) sb.append("\n");
        }
        lineNumbers.setText(sb.toString());
    }

    // ---------- التفاف الأسطر ----------

    private void applyWordWrap() {
        ViewGroup.LayoutParams params = codeEditor.getLayoutParams();
        params.width = wordWrap ? ViewGroup.LayoutParams.MATCH_PARENT : ViewGroup.LayoutParams.WRAP_CONTENT;
        codeEditor.setLayoutParams(params);
        hScrollEditor.setHorizontalScrollBarEnabled(!wordWrap);
    }

    // ---------- أزرار الرموز السريعة ----------

    private void setupToolButtons() {
        int tabSize = AppPreferences.getTabSize(this);
        StringBuilder tabSpaces = new StringBuilder();
        for (int i = 0; i < tabSize; i++) tabSpaces.append(' ');

        findViewById(R.id.btnTab).setOnClickListener(v -> insertAtCursor(tabSpaces.toString()));
        findViewById(R.id.btnBrace).setOnClickListener(v -> insertAtCursor("{}"));
        findViewById(R.id.btnParen).setOnClickListener(v -> insertAtCursor("()"));
        findViewById(R.id.btnQuote).setOnClickListener(v -> insertAtCursor("\"\""));
        findViewById(R.id.btnSemi).setOnClickListener(v -> insertAtCursor(";"));
        findViewById(R.id.btnUndo).setOnClickListener(v -> performUndo());
        findViewById(R.id.btnRedo).setOnClickListener(v -> performRedo());
    }

    private void insertAtCursor(String text) {
        int start = Math.max(codeEditor.getSelectionStart(), 0);
        codeEditor.getText().insert(start, text);
    }

    // ---------- البحث والاستبدال ----------

    private void setupFindBar() {
        findViewById(R.id.btnCloseFind).setOnClickListener(v -> {
            findBar.setVisibility(View.GONE);
            clearMatches();
        });
        findViewById(R.id.btnFindNext).setOnClickListener(v -> goToMatch(1));
        findViewById(R.id.btnFindPrev).setOnClickListener(v -> goToMatch(-1));
        findViewById(R.id.btnReplace).setOnClickListener(v -> replaceCurrent());
        findViewById(R.id.btnReplaceAll).setOnClickListener(v -> replaceAll());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                performSearch(s.toString());
            }
        });
    }

    private void performSearch(String query) {
        clearHighlightsOnly();
        matchPositions.clear();
        currentMatch = -1;
        if (query.isEmpty()) {
            matchCount.setText("0/0");
            return;
        }
        String content = codeEditor.getText().toString().toLowerCase(Locale.getDefault());
        String q = query.toLowerCase(Locale.getDefault());
        int idx = content.indexOf(q);
        while (idx >= 0) {
            matchPositions.add(idx);
            idx = content.indexOf(q, idx + 1);
        }
        if (!matchPositions.isEmpty()) {
            currentMatch = 0;
            highlightMatches(query.length());
            scrollToMatch(query.length());
        }
        matchCount.setText((matchPositions.isEmpty() ? 0 : currentMatch + 1) + "/" + matchPositions.size());
    }

    private void highlightMatches(int len) {
        Editable e = codeEditor.getText();
        BackgroundColorSpan[] spans = e.getSpans(0, e.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan sp : spans) e.removeSpan(sp);
        for (int i = 0; i < matchPositions.size(); i++) {
            int pos = matchPositions.get(i);
            int color = (i == currentMatch) ? 0x99FFA500 : 0x552196F3;
            e.setSpan(new BackgroundColorSpan(color), pos, pos + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private void clearHighlightsOnly() {
        Editable e = codeEditor.getText();
        BackgroundColorSpan[] spans = e.getSpans(0, e.length(), BackgroundColorSpan.class);
        for (BackgroundColorSpan sp : spans) e.removeSpan(sp);
    }

    private void clearMatches() {
        clearHighlightsOnly();
        matchPositions.clear();
        currentMatch = -1;
    }

    private void goToMatch(int direction) {
        if (matchPositions.isEmpty()) return;
        currentMatch = (currentMatch + direction + matchPositions.size()) % matchPositions.size();
        highlightMatches(etSearch.getText().length());
        scrollToMatch(etSearch.getText().length());
        matchCount.setText((currentMatch + 1) + "/" + matchPositions.size());
    }

    private void scrollToMatch(int len) {
        if (currentMatch < 0 || currentMatch >= matchPositions.size()) return;
        int pos = matchPositions.get(currentMatch);
        codeEditor.requestFocus();
        codeEditor.setSelection(pos, Math.min(pos + len, codeEditor.getText().length()));
        Layout layout = codeEditor.getLayout();
        if (layout != null) {
            int line = layout.getLineForOffset(pos);
            int y = layout.getLineTop(line);
            verticalScroll.smoothScrollTo(0, Math.max(0, y - 100));
        }
    }

    private void replaceCurrent() {
        if (matchPositions.isEmpty() || currentMatch < 0) return;
        int pos = matchPositions.get(currentMatch);
        int len = etSearch.getText().length();
        String replacement = etReplace.getText().toString();
        Editable e = codeEditor.getText();
        e.replace(pos, pos + len, replacement);
        performSearch(etSearch.getText().toString());
    }

    private void replaceAll() {
        String search = etSearch.getText().toString();
        String replacement = etReplace.getText().toString();
        if (search.isEmpty()) return;
        String content = codeEditor.getText().toString();
        String updated = content.replace(search, replacement);
        codeEditor.setText(updated);
        SyntaxHighlighter.highlight(this, codeEditor.getText(), lang);
        performSearch(search);
        Toast.makeText(this, R.string.toast_replaced, Toast.LENGTH_SHORT).show();
    }

    // ---------- القائمة العلوية ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_menu, menu);
        MenuItem preview = menu.findItem(R.id.action_preview);
        preview.setVisible(lang == SyntaxHighlighter.Lang.HTML);
        MenuItem wrapItem = menu.findItem(R.id.action_word_wrap);
        wrapItem.setChecked(wordWrap);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_save) {
            saveFile();
            return true;
        } else if (id == R.id.action_find) {
            findBar.setVisibility(findBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            return true;
        } else if (id == R.id.action_preview) {
            openPreview();
            return true;
        } else if (id == R.id.action_zoom_in) {
            fontSize = Math.min(fontSize + 2, 30);
            codeEditor.setTextSize(fontSize);
            lineNumbers.setTextSize(fontSize);
            AppPreferences.setFontSize(this, fontSize);
            return true;
        } else if (id == R.id.action_zoom_out) {
            fontSize = Math.max(fontSize - 2, 8);
            codeEditor.setTextSize(fontSize);
            lineNumbers.setTextSize(fontSize);
            AppPreferences.setFontSize(this, fontSize);
            return true;
        } else if (id == R.id.action_word_wrap) {
            wordWrap = !item.isChecked();
            item.setChecked(wordWrap);
            AppPreferences.setWordWrapEnabled(this, wordWrap);
            applyWordWrap();
            return true;
        } else if (id == R.id.action_rename) {
            showRenameDialog();
            return true;
        } else if (id == R.id.action_share) {
            shareFile();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void openPreview() {
        saveFile();
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra("html_content", codeEditor.getText().toString());
        startActivity(intent);
    }

    private void showRenameDialog() {
        final EditText input = new EditText(this);
        input.setText(currentFile.getName());
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rename_title)
                .setView(input)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;
                    File newFile = new File(currentFile.getParentFile(), newName);
                    if (currentFile.renameTo(newFile)) {
                        currentFile = newFile;
                        lang = SyntaxHighlighter.detectLang(currentFile.getName());
                        setDirty(dirty);
                        invalidateOptionsMenu();
                        SyntaxHighlighter.highlight(this, codeEditor.getText(), lang);
                        Toast.makeText(this, R.string.toast_renamed, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.toast_rename_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void shareFile() {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, currentFile.getName());
        share.putExtra(Intent.EXTRA_TEXT, codeEditor.getText().toString());
        startActivity(Intent.createChooser(share, getString(R.string.share_via)));
    }

    // ---------- دورة الحياة ----------

    @Override
    protected void onPause() {
        super.onPause();
        if (AppPreferences.isAutoSaveEnabled(this)) {
            saveFileSilently();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (AppPreferences.isAutoSaveEnabled(this)) saveFileSilently();
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (AppPreferences.isAutoSaveEnabled(this)) saveFileSilently();
        super.onBackPressed();
    }
}
