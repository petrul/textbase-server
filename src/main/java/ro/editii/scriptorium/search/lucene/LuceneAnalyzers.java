package ro.editii.scriptorium.search.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.analysis.ca.CatalanAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.da.DanishAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fi.FinnishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.hu.HungarianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.no.NorwegianAnalyzer;
import org.apache.lucene.analysis.pt.PortugueseAnalyzer;
import org.apache.lucene.analysis.ro.RomanianAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import ro.editii.scriptorium.model.Languages;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maps this app's Languages (see TeiFile.language / LanguageDetectionService)
 * to Lucene's built-in per-language analyzers - each does real stemming and
 * language-appropriate stopword removal, a real accuracy improvement over a
 * single generic analyzer for a 19-language corpus. Not every code here has
 * a Lucene analyzer (Latin, Breton) - those, and anything with no detected
 * language at all (documents imported before language detection existed),
 * fall back to the plain per-field-agnostic "content"/"head" fields.
 *
 * Lucene's built-in per-language analyzers don't fold diacritics, unlike
 * TextbaseAnalyzer (used for the generic content/head fields) - text/queries
 * are indexed and searched verbatim (with real diacritics) on a
 * per-language field, so its stemmer sees what it expects; the generic
 * fields remain the always-diacritics-optional fallback. See
 * LuceneIndexService.
 */
public class LuceneAnalyzers {

    private static final Map<Languages, Analyzer> BY_LANGUAGE = buildAnalyzerMap();

    private static Map<Languages, Analyzer> buildAnalyzerMap() {
        final Map<Languages, Analyzer> map = new EnumMap<>(Languages.class);
        map.put(Languages.BG, new BulgarianAnalyzer());
        map.put(Languages.CA, new CatalanAnalyzer());
        map.put(Languages.DA, new DanishAnalyzer());
        map.put(Languages.DE, new GermanAnalyzer());
        map.put(Languages.EN, new EnglishAnalyzer());
        map.put(Languages.ES, new SpanishAnalyzer());
        map.put(Languages.FI, new FinnishAnalyzer());
        map.put(Languages.FR, new FrenchAnalyzer());
        map.put(Languages.GR, new GreekAnalyzer());
        map.put(Languages.HU, new HungarianAnalyzer());
        map.put(Languages.IT, new ItalianAnalyzer());
        map.put(Languages.NL, new DutchAnalyzer());
        map.put(Languages.NO, new NorwegianAnalyzer());
        map.put(Languages.PT, new PortugueseAnalyzer());
        map.put(Languages.RO, new RomanianAnalyzer());
        map.put(Languages.RU, new RussianAnalyzer());
        map.put(Languages.ZH, new CJKAnalyzer());
        // LA (Latin), BR (Breton): no Lucene analyzer - intentionally absent.
        return map;
    }

    /**
     * @return the language-specific analyzer for this language, if Lucene
     * ships one - empty for languages it doesn't (Latin, Breton) or a null
     * (undetected/legacy) language.
     */
    public static Optional<Analyzer> analyzerFor(Languages language) {
        return Optional.ofNullable(language).map(BY_LANGUAGE::get);
    }

    /**
     * @return the field name to use for this base field ("content"/"head")
     * when this document has a language with a dedicated analyzer, e.g.
     * "content_ro" - null if there's no per-language variant to index/query
     * for this language (see analyzerFor).
     */
    public static String perLanguageFieldName(String baseField, Languages language) {
        if (analyzerFor(language).isEmpty())
            return null;
        return baseField + "_" + language.getISO639_1Code();
    }

    public static PerFieldAnalyzerWrapper perFieldAnalyzer(Analyzer defaultAnalyzer, String... baseFields) {
        final Map<String, Analyzer> fieldAnalyzers = new java.util.HashMap<>();
        for (Map.Entry<Languages, Analyzer> entry : BY_LANGUAGE.entrySet()) {
            for (String baseField : baseFields) {
                fieldAnalyzers.put(perLanguageFieldName(baseField, entry.getKey()), entry.getValue());
            }
        }
        return new PerFieldAnalyzerWrapper(defaultAnalyzer, fieldAnalyzers);
    }
}
