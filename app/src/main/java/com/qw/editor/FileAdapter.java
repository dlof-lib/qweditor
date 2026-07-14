package com.qw.editor;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    public interface FileClickListener {
        void onClick(File file);
    }

    private final List<File> files;
    private final FileClickListener onOpen;
    private final Consumer<File> onMore;

    public FileAdapter(List<File> files, FileClickListener onOpen, Consumer<File> onMore) {
        this.files = files;
        this.onOpen = onOpen;
        this.onMore = onMore;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        File file = files.get(position);

        holder.fileName.setText(file.getName());
        holder.fileMeta.setText(metaFor(holder.itemView.getContext(), file));
        applyIcon(holder, file);

        holder.itemView.setOnClickListener(v -> onOpen.onClick(file));
        holder.itemView.setOnLongClickListener(v -> {
            onMore.accept(file);
            return true;
        });
        holder.btnMore.setOnClickListener(v -> onMore.accept(file));
    }

    private String metaFor(android.content.Context ctx, File file) {
        if (file.isDirectory()) {
            String[] children = file.list();
            int count = children == null ? 0 : children.length;
            return count + (count == 1 ? " عنصر" : " عناصر");
        }
        String size = Formatter.formatShortFileSize(ctx, file.length());
        CharSequence when = DateUtils.getRelativeTimeSpanString(file.lastModified());
        return size + " • " + when;
    }

    /** وصف أيقونة اللغة: النص المعروض داخل الشارة، لون الخلفية، ولون النص. */
    private static final class LangIcon {
        final String label;
        final int colorRes;
        final int textColorRes;

        LangIcon(String label, int colorRes, int textColorRes) {
            this.label = label;
            this.colorRes = colorRes;
            this.textColorRes = textColorRes;
        }
    }

    private LangIcon langIconFor(File file) {
        if (file.isDirectory()) {
            return new LangIcon("📁", R.color.lang_folder, android.R.color.white);
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".py")) return new LangIcon("PY", R.color.lang_python, android.R.color.white);
        if (name.endsWith(".html") || name.endsWith(".htm")) return new LangIcon("<>", R.color.lang_html, android.R.color.white);
        if (name.endsWith(".css")) return new LangIcon("#", R.color.lang_css, android.R.color.white);
        if (name.endsWith(".js")) return new LangIcon("JS", R.color.lang_js, R.color.lang_js_text);
        if (name.endsWith(".php")) return new LangIcon("PHP", R.color.lang_php, android.R.color.white);
        if (name.endsWith(".json")) return new LangIcon("{ }", R.color.lang_json, android.R.color.white);
        if (name.endsWith(".md") || name.endsWith(".markdown")) return new LangIcon("MD", R.color.lang_md, android.R.color.white);
        if (name.endsWith(".kt")) return new LangIcon("KT", R.color.lang_kotlin, android.R.color.white);
        if (name.endsWith(".java")) return new LangIcon("JV", R.color.lang_java, android.R.color.white);
        if (name.endsWith(".xml")) return new LangIcon("XML", R.color.lang_xml, android.R.color.white);
        if (name.endsWith(".c") || name.endsWith(".cpp") || name.endsWith(".h")) return new LangIcon("C", R.color.lang_c, android.R.color.white);
        return new LangIcon("TXT", R.color.lang_txt, android.R.color.white);
    }

    private void applyIcon(FileViewHolder holder, File file) {
        Context ctx = holder.itemView.getContext();
        LangIcon icon = langIconFor(file);

        holder.fileIcon.setText(icon.label);
        holder.fileIcon.setTextColor(ContextCompat.getColor(ctx, icon.textColorRes));
        holder.fileIcon.setTextSize(icon.label.length() > 2 ? 11f : (file.isDirectory() ? 18f : 14f));
        ViewCompat.setBackgroundTintList(
                holder.fileIcon,
                ColorStateList.valueOf(ContextCompat.getColor(ctx, icon.colorRes)));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileIcon, fileMeta;
        ImageButton btnMore;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileMeta = itemView.findViewById(R.id.fileMeta);
            btnMore = itemView.findViewById(R.id.btnMore);
        }
    }
}
