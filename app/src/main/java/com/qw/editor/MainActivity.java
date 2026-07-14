package com.qw.editor;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends BaseActivity {

    /** اسم حزمة تطبيق DLoF الرسمي (org.dlof.reader) */
    private static final String DLOF_PACKAGE = "org.dlof.reader";

    private RecyclerView recyclerView;
    private TextView emptyText;
    private TextView breadcrumb;
    private EditText searchBox;
    private FileAdapter adapter;

    private File rootDir;
    private File currentDir;

    private final List<File> allFiles = new ArrayList<>();
    private final List<File> shownFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rootDir = new File(getExternalFilesDir(null), "QWEditorProjects");
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        currentDir = rootDir;

        recyclerView = findViewById(R.id.fileList);
        emptyText = findViewById(R.id.emptyText);
        breadcrumb = findViewById(R.id.breadcrumb);
        searchBox = findViewById(R.id.searchBox);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FileAdapter(shownFiles, this::openEntry, this::showItemMenu);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> showCreateChooser());

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { filterFiles(s.toString()); }
        });

        refreshFiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshFiles();
    }

    @Override
    public void onBackPressed() {
        if (!currentDir.equals(rootDir)) {
            currentDir = currentDir.getParentFile();
            refreshFiles();
        } else {
            super.onBackPressed();
        }
    }

    // ---------- القوائم العلوية ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        int mode = AppPreferences.getSortMode(this);
        if (mode == AppPreferences.SORT_DATE) menu.findItem(R.id.sort_date).setChecked(true);
        else if (mode == AppPreferences.SORT_TYPE) menu.findItem(R.id.sort_type).setChecked(true);
        else menu.findItem(R.id.sort_name).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sort_name) {
            AppPreferences.setSortMode(this, AppPreferences.SORT_NAME);
            item.setChecked(true);
            refreshFiles();
            return true;
        } else if (id == R.id.sort_date) {
            AppPreferences.setSortMode(this, AppPreferences.SORT_DATE);
            item.setChecked(true);
            refreshFiles();
            return true;
        } else if (id == R.id.sort_type) {
            AppPreferences.setSortMode(this, AppPreferences.SORT_TYPE);
            item.setChecked(true);
            refreshFiles();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------- تحميل وترتيب الملفات ----------

    private void refreshFiles() {
        allFiles.clear();
        File[] list = currentDir.listFiles();
        if (list != null) {
            allFiles.addAll(Arrays.asList(list));
        }
        sortFiles(allFiles);
        updateBreadcrumb();
        filterFiles(searchBox.getText() != null ? searchBox.getText().toString() : "");
    }

    private void sortFiles(List<File> files) {
        int mode = AppPreferences.getSortMode(this);
        Comparator<File> comparator;
        switch (mode) {
            case AppPreferences.SORT_DATE:
                comparator = (a, b) -> Long.compare(b.lastModified(), a.lastModified());
                break;
            case AppPreferences.SORT_TYPE:
                comparator = Comparator.comparing(this::extensionOf, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            default:
                comparator = Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER);
        }
        java.util.Collections.sort(files,
                Comparator.comparing((File f) -> !f.isDirectory()).thenComparing(comparator));
    }

    private String extensionOf(File f) {
        String name = f.getName();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private void updateBreadcrumb() {
        String rootPath = rootDir.getAbsolutePath();
        String curPath = currentDir.getAbsolutePath();
        String rel = curPath.equals(rootPath) ? "" : curPath.substring(rootPath.length() + 1);
        String label = "المشروع" + (rel.isEmpty() ? "" : " / " + rel.replace(File.separatorChar, '/'));
        breadcrumb.setText(label);
    }

    private void filterFiles(String query) {
        shownFiles.clear();
        String q = query.toLowerCase(Locale.getDefault()).trim();
        for (File f : allFiles) {
            if (q.isEmpty() || f.getName().toLowerCase(Locale.getDefault()).contains(q)) {
                shownFiles.add(f);
            }
        }
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(shownFiles.isEmpty() ? View.VISIBLE : View.GONE);
        emptyText.setText(allFiles.isEmpty()
                ? getString(R.string.empty_no_files)
                : getString(R.string.empty_no_results));
    }

    // ---------- إنشاء ملف / مجلد ----------

    private void showCreateChooser() {
        String[] options = {getString(R.string.action_new_file), getString(R.string.action_new_folder)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_new_title)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showCreateFileDialog();
                    else showCreateFolderDialog();
                })
                .show();
    }

    private void showCreateFileDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.hint_file_name);
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle(R.string.action_new_file)
                .setView(input)
                .setPositiveButton(R.string.action_create, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty_name, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File newFile = new File(currentDir, name);
                    if (newFile.exists()) {
                        Toast.makeText(this, R.string.error_name_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        if (newFile.createNewFile()) {
                            String template = FileTemplates.templateFor(name);
                            if (!template.isEmpty()) {
                                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(newFile)) {
                                    fos.write(template.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                }
                            }
                            refreshFiles();
                            openEntry(newFile);
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.error_generic, e.getMessage()), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showCreateFolderDialog() {
        final EditText input = new EditText(this);
        input.setHint(R.string.hint_folder_name);
        input.setPadding(40, 30, 40, 30);

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_new_folder_title)
                .setView(input)
                .setPositiveButton(R.string.action_create, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.error_empty_name, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    File newDir = new File(currentDir, name);
                    if (newDir.exists()) {
                        Toast.makeText(this, R.string.error_name_exists, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newDir.mkdirs()) {
                        refreshFiles();
                    } else {
                        Toast.makeText(this, R.string.error_empty_name, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    // ---------- فتح ملف / مجلد ----------

    private void openEntry(File file) {
        if (file.isDirectory()) {
            currentDir = file;
            refreshFiles();
        } else if (isDlofPackage(file)) {
            openWithDlof(file);
        } else {
            Intent intent = new Intent(this, EditorActivity.class);
            intent.putExtra("file_path", file.getAbsolutePath());
            startActivity(intent);
        }
    }

    private boolean isDlofPackage(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".dlofpkg");
    }

    /** يفتح ملف .dlofpkg في تطبيق DLoF الرسمي، أو يطلب تثبيته إن لم يكن موجودًا. */
    private void openWithDlof(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/octet-stream");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showDlofRequiredDialog();
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_dlof_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void showDlofRequiredDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dlof_required_title)
                .setMessage(R.string.dlof_required_message)
                .setPositiveButton(R.string.dlof_required_get_app, (d, w) -> openDlofOnPlayStore())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void openDlofOnPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + DLOF_PACKAGE)));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + DLOF_PACKAGE)));
        }
    }

    // ---------- قائمة عنصر واحد (نسخ / حذف / تسمية / تفاصيل) ----------

    private void showItemMenu(File file) {
        View anchor = recyclerView;
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, R.string.menu_open);
        popup.getMenu().add(0, 2, 1, R.string.menu_rename);
        if (!file.isDirectory()) {
            popup.getMenu().add(0, 3, 2, R.string.menu_duplicate);
        }
        popup.getMenu().add(0, 4, 3, R.string.menu_details);
        popup.getMenu().add(0, 5, 4, R.string.menu_delete);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                openEntry(file);
            } else if (id == 2) {
                renameFile(file);
            } else if (id == 3) {
                duplicateFile(file);
            } else if (id == 4) {
                showDetails(file);
            } else if (id == 5) {
                confirmDelete(file);
            }
            return true;
        });
        popup.show();
    }

    private void confirmDelete(File file) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_delete_title)
                .setMessage(getString(R.string.dialog_delete_message, file.getName()))
                .setPositiveButton(R.string.action_delete, (d, w) -> {
                    deleteRecursively(file);
                    refreshFiles();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        file.delete();
    }

    private void renameFile(File file) {
        final EditText input = new EditText(this);
        input.setText(file.getName());
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_rename_title)
                .setView(input)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;
                    File newFile = new File(currentDir, newName);
                    if (file.renameTo(newFile)) {
                        refreshFiles();
                    } else {
                        Toast.makeText(this, R.string.toast_rename_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void duplicateFile(File file) {
        try {
            String name = file.getName();
            String base = name, ext = "";
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                base = name.substring(0, dot);
                ext = name.substring(dot);
            }
            File copy = new File(currentDir, base + "_copy" + ext);
            int n = 1;
            while (copy.exists()) {
                copy = new File(currentDir, base + "_copy" + (++n) + ext);
            }
            copyFile(file, copy);
            refreshFiles();
            Toast.makeText(this, R.string.toast_duplicated, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_generic, e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(File source, File dest) throws java.io.IOException {
        try (java.io.FileInputStream in = new java.io.FileInputStream(source);
             java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private void showDetails(File file) {
        DateFormat df = DateFormat.getDateTimeInstance();
        String message = file.getAbsolutePath().replace(rootDir.getAbsolutePath(), "المشروع") + "\n\n" +
                (file.isDirectory()
                        ? "مجلد"
                        : android.text.format.Formatter.formatShortFileSize(this, file.length())) +
                "\n" + df.format(new Date(file.lastModified()));

        new AlertDialog.Builder(this)
                .setTitle(file.getName())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
