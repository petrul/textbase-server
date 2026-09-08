package ro.editii.scriptorium

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import ro.editii.scriptorium.tei.TeiDirRepoImpl
import ro.editii.scriptorium.tei.TeiRepo

/**
 * Overrides TestConfig's teiRepo bean to point at "testrepo-search"
 * instead of the shared "testrepo" - a small, multi-language, self-contained
 * fixture set (6 real public-domain works: RO/EN/FR/DE/ES/IT, ~136KB total)
 * checked into src/test/resources/testrepo-search. Used by tests that need
 * language *diversity* and/or a fast reimport (the shared testrepo is 4
 * complete Romanian classics, 16k+ paragraphs, ~1 minute to reimport+reindex)
 * rather than the shared fixture's exact content - SearchITest and others
 * already hardcode specific content from testrepo, so replacing it wholesale
 * isn't a safe/small change.
 *
 * Import this AFTER TestConfig in a test class's @SpringBootTest(classes=...)
 * list (with spring.main.allow-bean-definition-overriding=true) so this
 * teiRepo bean wins the override - verify with an exact-count assertion
 * after import (see LuceneSearchITest.beforeAll) rather than assuming it.
 */
@TestConfiguration
class MultilangTeiRepoConfig {
    @Bean
    TeiRepo teiRepo() {
        final dirname = Util.urlToFileString(this.class.classLoader.getResource("testrepo-search"))
        return new TeiDirRepoImpl(dirname)
    }
}
