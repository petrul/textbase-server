package ro.editii.scriptorium.service;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ro.editii.scriptorium.model.Languages;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Detects a TEI document's language from its own text at import time (see
 * TeiFileDbService.importTeiFile) - this is what replaced guessing the
 * language from the file's directory path (TeiDirRepoImpl.getLanguageHint),
 * which silently returned null for anything not filed under a
 * language-coded folder and had no relationship to the document's actual
 * content.
 *
 * Lingua ships its own per-language n-gram models rather than calling a
 * detection library not built for the JVM (e.g. Google's cld/cld3, which
 * would need a native/JNI binding) - one dependency, no external service,
 * no native library to build/ship.
 */
@Service
@Log4j2
public class LanguageDetectionService {

    // Long documents don't need to be fed in full for reliable detection -
    // a representative prefix is enough, and keeps this fast even for a
    // large TEI file (some are whole books).
    private static final int SAMPLE_CHARS = 3000;

    private final LanguageDetector detector;
    private final Map<Language, Languages> linguaToOurs;

    public LanguageDetectionService() {
        final Map<Language, Languages> mapping = new EnumMap<>(Language.class);
        for (Languages ours : Languages.values()) {
            try {
                final Language lingua = Language.valueOf(ours.getEnName().toUpperCase());
                mapping.put(lingua, ours);
            } catch (IllegalArgumentException e) {
                // Lingua has no model for this one (e.g. Latin, Breton) -
                // detect() below will just never return it.
                log.info("No Lingua language model for {} ({}) - it will never be auto-detected.",
                        ours, ours.getEnName());
            }
        }
        this.linguaToOurs = mapping;
        this.detector = LanguageDetectorBuilder.fromLanguages(mapping.keySet().toArray(new Language[0])).build();
    }

    /**
     * @param rawTeiXml the document's raw TEI XML (tags are stripped with a
     *                  cheap regex rather than a real parse - a bit of
     *                  markup noise doesn't meaningfully affect detection on
     *                  a document-sized sample)
     * @return the detected language, or empty if the text is too short/
     * ambiguous for a confident call, or its detected language isn't one
     * this app models at all (see Languages)
     */
    public Optional<Languages> detect(String rawTeiXml) {
        if (rawTeiXml == null || rawTeiXml.isBlank())
            return Optional.empty();

        final String plain = rawTeiXml.replaceAll("<[^>]+>", " ");
        final String sample = plain.length() > SAMPLE_CHARS ? plain.substring(0, SAMPLE_CHARS) : plain;
        if (sample.isBlank())
            return Optional.empty();

        final Language detected = this.detector.detectLanguageOf(sample);
        if (detected == Language.UNKNOWN)
            return Optional.empty();

        return Optional.ofNullable(this.linguaToOurs.get(detected));
    }
}
