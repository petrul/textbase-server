package ro.editii.scriptorium.dav;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import ro.editii.scriptorium.model.Languages;
import ro.editii.scriptorium.model.TeiDiv;
import ro.editii.scriptorium.model.TeiFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class DavExportControllerTest {
    private final DavExportService service = mock(DavExportService.class);
    private final DavExportRenderer renderer = mock(DavExportRenderer.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = standaloneSetup(new DavExportController(service, renderer)).build();
    }

    @Test
    void propfindReturnsDavMultistatusAndKeepsMountParametersInHrefs() throws Exception {
        final DavResource root = new DavResource(List.of(), "Textbase", true, null);
        final DavResource language = new DavResource(List.of("fr"), "fr", true, null);
        when(service.resolve(eq(List.of()), any())).thenReturn(Optional.of(root));
        when(service.children(eq(root), any())).thenReturn(List.of(language));

        mvc.perform(request(HttpMethod.valueOf("PROPFIND"),
                        "/dav?format=xhtml&fragmentation=1.1&lang=fr").header("Depth", "1"))
                .andExpect(status().is(207))
                .andExpect(header().string("DAV", "1"))
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/dav/fr/?format=xhtml&amp;fragmentation=1.1&amp;lang=fr")));
    }

    @Test
    void pathConfiguredMountKeepsOptionsWithoutDependingOnAQueryString() throws Exception {
        final DavResource root = new DavResource(List.of(), "Textbase", true, null);
        final DavResource language = new DavResource(List.of("fr"), "fr", true, null);
        when(service.resolve(eq(List.of()), any())).thenReturn(Optional.of(root));
        when(service.children(eq(root), any())).thenReturn(List.of(language));

        mvc.perform(request(HttpMethod.valueOf("PROPFIND"),
                        "/dav/_export/txt/1.1/fr/perrault/").header("Depth", "1"))
                .andExpect(status().is(207))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/dav/_export/txt/1.1/fr/perrault/fr/")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("?format="))));

        verify(service).resolve(eq(List.of()), org.mockito.ArgumentMatchers.argThat(options ->
                options.format() == DavExportFormat.TXT
                        && options.fragmentationDepth() == 2
                        && options.language() == ro.editii.scriptorium.model.Languages.FR
                        && "perrault".equals(options.author())));
    }

    @Test
    void getAndHeadExposeTheSelectedRepresentation() throws Exception {
        final TeiFile fileData = new TeiFile();
        fileData.setLanguage(Languages.RO);
        final TeiDiv div = new TeiDiv();
        div.setTeiFile(fileData);
        div.setXpath("/tei:div");
        div.setUrlFragment("work");
        final DavResource file = new DavResource(List.of("ro", "writer", "work.json"), "Work", false, div);
        when(service.resolve(eq(file.path()), any())).thenReturn(Optional.of(file));
        when(renderer.render(eq(div), eq(DavExportFormat.JSON), any()))
                .thenReturn("{\"work\":true}".getBytes(StandardCharsets.UTF_8));

        mvc.perform(request(HttpMethod.GET, "/dav/ro/writer/work.json?format=json"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json("{\"work\":true}"));
        mvc.perform(request(HttpMethod.HEAD, "/dav/ro/writer/work.json?format=json"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "13"))
                .andExpect(content().string(""));
    }

    @Test
    void rejectsInfiniteListingsInvalidParametersAndWrites() throws Exception {
        final DavResource root = new DavResource(List.of(), "Textbase", true, null);
        when(service.resolve(eq(List.of()), any())).thenReturn(Optional.of(root));

        mvc.perform(request(HttpMethod.valueOf("PROPFIND"), "/dav").header("Depth", "infinity"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("propfind-finite-depth")));
        mvc.perform(request(HttpMethod.GET, "/dav?format=pdf"))
                .andExpect(status().isBadRequest());
        mvc.perform(request(HttpMethod.PUT, "/dav"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string(HttpHeaders.ALLOW, "OPTIONS, PROPFIND, GET, HEAD"));
    }
}
