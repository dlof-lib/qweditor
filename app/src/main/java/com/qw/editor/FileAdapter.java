package com.qw.editor;

import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        boolean isDir = file.isDirectory();

        holder.fileName.setText(file.getName());
        holder.fileIcon.setText(iconFor(file));
        holder.fileMeta.setText(metaFor(holder.itemView.getContext(), file));

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

    private String iconFor(File file) {
        if (file.isDirectory()) return "📁";
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".py")) return "PY";
        if (name.endsWith(".html") || name.endsWith(".htm")) return "<>";
        if (name.endsWith(".css")) return "#";
        if (name.endsWith(".js")) return "JS";
        if (name.endsWith(".php")) return "PHP";
        if (name.endsWith(".json")) return "{ }";
        if (name.endsWith(".md") || name.endsWith(".markdown")) return "MD";
        if (name.endsWith(".java") || name.endsWith(".kt")) return "JV";
        if (name.endsWith(".xml")) return "XML";
        if (name.endsWith(".c") || name.endsWith(".cpp") || name.endsWith(".h")) return "C";
        return "TXT";
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
