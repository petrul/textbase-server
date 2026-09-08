package ro.editii.scriptorium.search.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * The corpus is mostly Romanian (plus 18 other languages, see
 * {@link ro.editii.scriptorium.model.Languages}), so a plain
 * {@code StandardAnalyzer} is the wrong default on two counts: its built-in
 * stopword list is English-only (would silently bias/skip matches in every
 * other language), and it doesn't fold Romanian diacritics
 * (ă/â/î/ș/ț -> a/a/i/s/t), so a reader typing without diacritics (very
 * common in practice) would get no matches at all.
 *
 * This tokenizes with the same StandardTokenizer, lowercases, and folds
 * diacritics to ASCII - both indexing and querying go through the same
 * analyzer (see LuceneIndexService), so a diacritics-free query still
 * matches diacritics-bearing indexed text. No stopword removal, since it's
 * not obviously safe to reuse a single-language stopword list across 19
 * languages.
 */
public class TextbaseAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        final StandardTokenizer tokenizer = new StandardTokenizer();
        TokenStream stream = new LowerCaseFilter(tokenizer);
        stream = new ASCIIFoldingFilter(stream);
        return new TokenStreamComponents(tokenizer, stream);
    }
}
