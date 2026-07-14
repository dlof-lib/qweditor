package com.qw.editor;

import java.util.Locale;

/**
 * يوفر محتوى ابتدائيًا (Boilerplate) للملفات الجديدة حسب امتدادها.
 */
public final class FileTemplates {

    private FileTemplates() {}

    public static String templateFor(String fileName) {
        String n = fileName.toLowerCase(Locale.ROOT);
        if (n.endsWith(".html") || n.endsWith(".htm")) {
            return "<!DOCTYPE html>\n" +
                    "<html lang=\"ar\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>مستند جديد</title>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    \n" +
                    "</body>\n" +
                    "</html>\n";
        }
        if (n.endsWith(".css")) {
            return "/* أنماط الصفحة */\n\nbody {\n    margin: 0;\n    font-family: sans-serif;\n}\n";
        }
        if (n.endsWith(".js")) {
            return "// ملف جافاسكريبت جديد\n\n(function () {\n    \n})();\n";
        }
        if (n.endsWith(".py")) {
            return "# -*- coding: utf-8 -*-\n\n\ndef main():\n    pass\n\n\nif __name__ == \"__main__\":\n    main()\n";
        }
        if (n.endsWith(".php")) {
            return "<?php\n\n\n";
        }
        if (n.endsWith(".json")) {
            return "{\n    \n}\n";
        }
        if (n.endsWith(".md") || n.endsWith(".markdown")) {
            return "# عنوان\n\nاكتب هنا...\n";
        }
        if (n.endsWith(".xml")) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n    \n</root>\n";
        }
        if (n.endsWith(".java")) {
            return "public class NewClass {\n\n}\n";
        }
        if (n.endsWith(".c") || n.endsWith(".cpp") || n.endsWith(".h")) {
            return "#include <stdio.h>\n\nint main() {\n    return 0;\n}\n";
        }
        return "";
    }
}
