package ro.editii.scriptorium.dav;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ro.editii.scriptorium.model.Author;
import ro.editii.scriptorium.model.TeiDiv;
import ro.editii.scriptorium.model.TeiFile;
import ro.editii.scriptorium.tei.TeiRepo;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DavExportRendererTest {
    private static final String TEI = """
            <TEI xmlns="http://www.tei-c.org/ns/1.0"><text><body>
              <div type="div1"><head>Complete Work</head><p>Opening text.</p>
                <div type="div2"><head>Nested Chapter</head><p>Nested text.</p></div>
              </div>
            </body></text></TEI>
            """;

    @Test
    void rendersEverySupportedFormatAndKeepsTheCompleteSubtree() throws Exception {
        final TeiRepo teiRepo = mock(TeiRepo.class);
        when(teiRepo.getStreamForName("dav-renderer-fixture.xml")).thenAnswer(it ->
                new ByteArrayInputStream(TEI.getBytes(StandardCharsets.UTF_8)));
        final DavExportRenderer renderer = new DavExportRenderer(teiRepo);
        final TeiDiv div = div();

        final String txt = string(renderer.render(div, DavExportFormat.TXT, "/dav/complete_work.txt"));
        final String json = string(renderer.render(div, DavExportFormat.JSON, "/dav/complete_work.json"));
        final String xml = string(renderer.render(div, DavExportFormat.XML, "/dav/complete_work.xml"));
        final String xhtml = string(renderer.render(div, DavExportFormat.XHTML, "/dav/complete_work.xhtml"));

        assertThat(txt).contains("Opening text.", "Nested text.");
        assertThat(xml).contains("Complete Work", "Nested Chapter", "Nested text.");
        assertThat(new ObjectMapper().readTree(json)).isNotNull();
        assertThat(json).contains("Nested text.");

        final var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final var document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xhtml.getBytes(StandardCharsets.UTF_8)));
        assertThat(document.getDocumentElement().getLocalName()).isEqualTo("html");
        assertThat(document.getDocumentElement().getNamespaceURI()).isEqualTo("http://www.w3.org/1999/xhtml");
        assertThat(xhtml).contains("Opening text.", "Nested text.");
    }

    private String string(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private TeiDiv div() {
        final Author author = new Author();
        author.setStrId("writer");
        author.setFirstName("Ada");
        author.setLastName("Writer");

        final TeiFile file = new TeiFile();
        file.setFilename("dav-renderer-fixture.xml");
        file.setAuthors(List.of(author));

        final TeiDiv div = new TeiDiv();
        div.setTeiFile(file);
        div.setXpath("/tei:div");
        div.setHead("Complete Work");
        div.setUrlFragment("complete_work");
        return div;
    }
}
