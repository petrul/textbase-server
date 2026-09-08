package ro.editii.scriptorium.search.lucene;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ro.editii.scriptorium.dao.TeiDivRepository;
import ro.editii.scriptorium.model.Languages;
import ro.editii.scriptorium.model.TeiDiv;
import ro.editii.scriptorium.model.TeiElem;
import ro.editii.scriptorium.search.LuceneHit;
import ro.editii.scriptorium.service.ControllerTool;
import ro.editii.scriptorium.service.DivService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-text search over the corpus, at paragraph granularity (same grain as
 * the Milvus vector index's tb_paras_* collections - see VectorConfig) since
 * that's the unit a reader actually wants a hit to point at.
 *
 * Body text isn't stored relationally (see TeiElem.getNode()), so building
 * the index means re-deriving each paragraph's text the same way ann() does
 * for a single element: TeiElem -> toElemInfo() -> ControllerTool's XSLT
 * transform. Unlike Milvus (which stores no text, only vectors, and
 * resolves content separately at query time via ContentResolver), Lucene
 * stores the text itself, so a search hit needs no second resolution step.
 *
 * A rebuild is a full recreate (IndexWriterConfig.OpenMode.CREATE), not an
 * incremental update - simplest correct model for now, at the cost of
 * re-deriving text for the whole corpus every time. Readers keep working
 * against the previous index contents while a rebuild is in progress
 * (Lucene's normal point-in-time-reader guarantee); refreshIfNeeded() below
 * only swaps them over to the new segments once the rebuild has committed.
 */
@Service
@Log4j2
public class LuceneIndexService {

    public static final String FIELD_URL = "url";
    public static final String FIELD_CONTENT = "content";
    public static final String FIELD_HEAD = "head";

    private static final int OPERA_PAGE_SIZE = 20;
    private static final int SEARCH_CONTENT_BOOST = 1;
    private static final int SEARCH_HEAD_BOOST = 3;

    private final Path indexDir;
    private final Directory directory;
    private final Analyzer analyzer;
    private final TeiDivRepository teiDivRepository;
    private final DivService divService;
    private final ControllerTool controllerTool;

    private final Object rebuildLock = new Object();
    private volatile SearcherManager searcherManager;

    public LuceneIndexService(@Value("${lucene.index.dir}") String indexDir,
                               TeiDivRepository teiDivRepository,
                               DivService divService,
                               ControllerTool controllerTool) throws IOException {
        this.indexDir = Path.of(indexDir);
        Files.createDirectories(this.indexDir);
        this.directory = FSDirectory.open(this.indexDir);
        // Language-aware: FIELD_CONTENT/FIELD_HEAD (always populated) use
        // TextbaseAnalyzer; a document whose TeiFile.language has a Lucene
        // built-in analyzer (see LuceneAnalyzers) also gets extra
        // "content_<lang>"/"head_<lang>" fields analyzed with real
        // per-language stemming/stopwords, for documents imported after
        // language detection was added (see TeiFileDbService).
        this.analyzer = LuceneAnalyzers.perFieldAnalyzer(new TextbaseAnalyzer(), FIELD_CONTENT, FIELD_HEAD);
        this.teiDivRepository = teiDivRepository;
        this.divService = divService;
        this.controllerTool = controllerTool;

        if (DirectoryReader.indexExists(this.directory)) {
            this.searcherManager = new SearcherManager(this.directory, null);
            log.info("Lucene full-text index found at {} - /api/search/lucene is available.", this.indexDir);
        } else {
            log.warn("No Lucene index at {} yet - /api/search/lucene will return empty results until "
                    + "POST /api/admin/lucene/reindex builds one.", this.indexDir);
        }
    }

    public boolean isAvailable() {
        return this.searcherManager != null;
    }

    /**
     * Walks every opus (root TeiDiv, see TeiDivRepository.findOpera) and
     * indexes each of its paragraphs (DivService.getParagraphs already
     * walks the whole work's Toc, sub-chapters included). One bad paragraph
     * (e.g. an XSLT transform failure) is logged and skipped rather than
     * aborting the whole rebuild - same reasoning as the per-message
     * isolation used elsewhere for batch/streaming work.
     */
    public int rebuildIndex() {
        synchronized (this.rebuildLock) {
            final StopWatch watch = new StopWatch();
            watch.start();
            int indexed = 0;
            int skipped = 0;

            try (IndexWriter writer = new IndexWriter(this.directory,
                    new IndexWriterConfig(this.analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE))) {

                int pageNr = 0;
                Page<TeiDiv> opera;
                do {
                    opera = this.teiDivRepository.findOpera(PageRequest.of(pageNr, OPERA_PAGE_SIZE));
                    for (TeiDiv opus : opera) {
                        for (TeiElem para : this.divService.getParagraphs(opus)) {
                            try {
                                final Document doc = toDocument(para);
                                if (doc != null) {
                                    writer.addDocument(doc);
                                    indexed++;
                                }
                            } catch (Exception e) {
                                skipped++;
                                log.warn("Skipping paragraph while building the Lucene index ({}): {}",
                                        safeCompletePath(para), e.getMessage());
                            }
                        }
                    }
                    pageNr++;
                } while (opera.hasNext());

                writer.commit();
            } catch (IOException e) {
                throw new RuntimeException("Failed to rebuild the Lucene index at " + this.indexDir, e);
            }

            try {
                if (this.searcherManager == null) {
                    this.searcherManager = new SearcherManager(this.directory, null);
                } else {
                    this.searcherManager.maybeRefreshBlocking();
                }
            } catch (IOException e) {
                throw new RuntimeException("Lucene index rebuilt but failed to open/refresh a searcher for it", e);
            }

            watch.stop();
            log.info("Rebuilt Lucene index: {} paragraphs indexed, {} skipped, took {}", indexed, skipped, watch);
            return indexed;
        }
    }

    private Document toDocument(TeiElem para) {
        final String text = this.controllerTool.teiElemToString(para.toElemInfo());
        if (text == null || text.isBlank())
            return null;

        // Kept as the original (accented) text, not folded - it's both what
        // gets stored/returned verbatim to callers (LuceneHit.content) and
        // what the per-language field's real stemmer wants to see; the
        // generic FIELD_CONTENT/FIELD_HEAD fields fold diacritics on their
        // own at the token level (TextbaseAnalyzer's ASCIIFoldingFilter),
        // so a diacritics-free query still matches through those.
        final String head = para.getDiv().getHead();
        final Languages language = documentLanguage(para);

        final Document doc = new Document();
        doc.add(new StringField(FIELD_URL, para.getCompletePath(), Field.Store.YES));
        doc.add(new TextField(FIELD_CONTENT, text, Field.Store.YES));
        addPerLanguageField(doc, FIELD_CONTENT, text, language);
        if (head != null && !head.isBlank()) {
            doc.add(new TextField(FIELD_HEAD, head, Field.Store.YES));
            addPerLanguageField(doc, FIELD_HEAD, head, language);
        }
        return doc;
    }

    private void addPerLanguageField(Document doc, String baseField, String value, Languages language) {
        final String fieldName = LuceneAnalyzers.perLanguageFieldName(baseField, language);
        if (fieldName != null)
            // Not stored - it's the same text already stored on baseField,
            // this field only exists to be searched with a better analyzer.
            doc.add(new TextField(fieldName, value, Field.Store.NO));
    }

    private static Languages documentLanguage(TeiElem para) {
        try {
            return para.getOpus().getTeiFile().getLanguage();
        } catch (Exception e) {
            return null;
        }
    }

    private static String safeCompletePath(TeiElem elem) {
        try {
            return elem.getCompletePath();
        } catch (Exception e) {
            return "xpath=" + elem.getXpath();
        }
    }

    /**
     * Every base field, plus every language-specific variant Lucene has an
     * analyzer for (see LuceneAnalyzers) - a query doesn't know in advance
     * which language(s) it'll match, so it's cheaper to always search the
     * full set (Lucene skips fields with no matching terms fast) than to
     * try to detect the query's own language first.
     */
    private static Map<String, Float> searchFieldsAndBoosts() {
        final Map<String, Float> fields = new LinkedHashMap<>();
        fields.put(FIELD_CONTENT, (float) SEARCH_CONTENT_BOOST);
        fields.put(FIELD_HEAD, (float) SEARCH_HEAD_BOOST);
        for (Languages language : Languages.values()) {
            final String contentField = LuceneAnalyzers.perLanguageFieldName(FIELD_CONTENT, language);
            if (contentField != null) {
                fields.put(contentField, (float) SEARCH_CONTENT_BOOST);
                fields.put(LuceneAnalyzers.perLanguageFieldName(FIELD_HEAD, language), (float) SEARCH_HEAD_BOOST);
            }
        }
        return fields;
    }

    public List<LuceneHit> search(String q, int limit) {
        if (!this.isAvailable())
            return List.of();

        final Query query;
        try {
            final Map<String, Float> fieldsAndBoosts = searchFieldsAndBoosts();
            final var parser = new MultiFieldQueryParser(
                    fieldsAndBoosts.keySet().toArray(new String[0]),
                    this.analyzer,
                    fieldsAndBoosts);
            // Escaped so a user's free-text query is always treated as plain
            // words (like /divHeads or /authors, plain "contains" matching),
            // never as Lucene query syntax they didn't ask to opt into. Not
            // folded here - each target field's own analyzer decides
            // whether to fold diacritics (see toDocument/LuceneAnalyzers).
            query = parser.parse(QueryParser.escape(q));
        } catch (ParseException e) {
            log.warn("Could not parse Lucene query [{}]: {}", q, e.getMessage());
            return List.of();
        }

        try {
            final IndexSearcher searcher = this.searcherManager.acquire();
            try {
                final TopDocs topDocs = searcher.search(query, limit);
                final List<LuceneHit> hits = new ArrayList<>(topDocs.scoreDocs.length);
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    final Document doc = searcher.storedFields().document(scoreDoc.doc);
                    hits.add(new LuceneHit(
                            doc.get(FIELD_URL),
                            scoreDoc.score,
                            doc.get(FIELD_CONTENT),
                            doc.get(FIELD_HEAD)));
                }
                return hits;
            } finally {
                this.searcherManager.release(searcher);
            }
        } catch (IOException e) {
            log.warn("Lucene search failed for [{}]: {}", q, e.getMessage());
            return List.of();
        }
    }
}
