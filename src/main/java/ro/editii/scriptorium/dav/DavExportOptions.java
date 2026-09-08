package ro.editii.scriptorium.dav;

import ro.editii.scriptorium.model.Languages;

import java.util.regex.Pattern;

public record DavExportOptions(
        DavExportFormat format,
        int fragmentationDepth,
        String fragmentation,
        Languages language,
        String author
) {
    private static final Pattern FRAGMENTATION = Pattern.compile("1(?:\\.1){0,2}");

    public static DavExportOptions from(String format, String fragmentation, String lang, String author) {
        final String level = fragmentation == null || fragmentation.isBlank() ? "1" : fragmentation.trim();
        if (!FRAGMENTATION.matcher(level).matches())
            throw new IllegalArgumentException("fragmentation must be one of 1, 1.1, 1.1.1");

        Languages language = null;
        if (lang != null && !lang.isBlank()) {
            language = Languages.from(lang.trim());
            if (language == null)
                throw new IllegalArgumentException("unknown language: " + lang);
        }

        final String authorFilter = author == null || author.isBlank() ? null : author.trim();
        return new DavExportOptions(
                DavExportFormat.from(format == null || format.isBlank() ? "txt" : format),
                level.split("\\.").length,
                level,
                language,
                authorFilter);
    }
}
