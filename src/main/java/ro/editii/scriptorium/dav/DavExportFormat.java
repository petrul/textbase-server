package ro.editii.scriptorium.dav;

import org.springframework.http.MediaType;

import java.util.Locale;

public enum DavExportFormat {
    TXT("txt", MediaType.TEXT_PLAIN_VALUE),
    JSON("json", MediaType.APPLICATION_JSON_VALUE),
    XML("xml", MediaType.APPLICATION_XML_VALUE),
    XHTML("xhtml", "application/xhtml+xml");

    private final String extension;
    private final String mediaType;

    DavExportFormat(String extension, String mediaType) {
        this.extension = extension;
        this.mediaType = mediaType;
    }

    public String extension() {
        return extension;
    }

    public String contentType() {
        return mediaType + "; charset=utf-8";
    }

    public static DavExportFormat from(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("format must be one of txt, json, xml, xhtml");
        }
    }
}
