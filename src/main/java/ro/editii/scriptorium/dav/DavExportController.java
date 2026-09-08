package ro.editii.scriptorium.dav;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriUtils;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Controller
@Hidden // OpenAPI cannot model DAV's PROPFIND method; the protocol is documented in README.md.
@RequiredArgsConstructor
public class DavExportController {
    private static final String DAV_NAMESPACE = "DAV:";
    private static final String ALLOW = "OPTIONS, PROPFIND, GET, HEAD";

    private final DavExportService exportService;
    private final DavExportRenderer renderer;

    @RequestMapping(value = {"/dav", "/dav/**"}, method = RequestMethod.OPTIONS)
    public void options(HttpServletResponse response) {
        response.setHeader("DAV", "1");
        response.setHeader(HttpHeaders.ALLOW, ALLOW);
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    @RequestMapping({"/dav", "/dav/**"})
    @Transactional(readOnly = true)
    public void dav(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("DAV", "1");
        response.setHeader(HttpHeaders.ALLOW, ALLOW);

        final DavRequest davRequest;
        try {
            davRequest = parseRequest(request);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            return;
        }

        final DavExportOptions options = davRequest.options();
        final DavResource resource = exportService.resolve(davRequest.resourcePath(), options).orElse(null);
        if (resource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        switch (request.getMethod()) {
            case "OPTIONS" -> response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            case "PROPFIND" -> propfind(resource, davRequest, request, response);
            case "GET" -> get(resource, options, request, response, false);
            case "HEAD" -> get(resource, options, request, response, true);
            default -> response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "The Textbase DAV export is read-only");
        }
    }

    private void get(DavResource resource, DavExportOptions options, HttpServletRequest request,
                     HttpServletResponse response, boolean headOnly) throws IOException {
        if (resource.collection()) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Collections are listed with PROPFIND");
            return;
        }

        final byte[] content = renderer.render(resource.div(), options.format(), canonicalContentUri(resource));
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(options.format().contentType());
        response.setContentLength(content.length);
        response.setHeader(HttpHeaders.ETAG, etag(resource, options));
        response.setHeader(HttpHeaders.LAST_MODIFIED, lastModified(resource));
        if (!headOnly)
            response.getOutputStream().write(content);
    }

    private void propfind(DavResource resource, DavRequest davRequest, HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        final DavExportOptions options = davRequest.options();
        final String depth = request.getHeader("Depth");
        if ("infinity".equalsIgnoreCase(depth)) {
            finiteDepthError(response);
            return;
        }
        if (depth != null && !depth.equals("0") && !depth.equals("1")) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Depth must be 0 or 1");
            return;
        }

        response.setStatus(207);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml; charset=utf-8");
        try {
            final XMLStreamWriter xml = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8.name());
            xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            xml.writeStartElement("D", "multistatus", DAV_NAMESPACE);
            xml.writeNamespace("D", DAV_NAMESPACE);
            writeResponse(xml, resource, davRequest, request);
            if (!"0".equals(depth) && resource.collection()) {
                for (DavResource child : exportService.children(resource, options))
                    writeResponse(xml, child, davRequest, request);
            }
            xml.writeEndElement();
            xml.writeEndDocument();
            xml.flush();
        } catch (XMLStreamException e) {
            throw new IOException("Could not write DAV response", e);
        }
    }

    private void writeResponse(XMLStreamWriter xml, DavResource resource, DavRequest davRequest,
                               HttpServletRequest request) throws XMLStreamException {
        final DavExportOptions options = davRequest.options();
        xml.writeStartElement("D", "response", DAV_NAMESPACE);
        element(xml, "href", href(resource, davRequest, request));
        xml.writeStartElement("D", "propstat", DAV_NAMESPACE);
        xml.writeStartElement("D", "prop", DAV_NAMESPACE);
        element(xml, "displayname", resource.displayName());
        xml.writeStartElement("D", "resourcetype", DAV_NAMESPACE);
        if (resource.collection())
            xml.writeEmptyElement("D", "collection", DAV_NAMESPACE);
        xml.writeEndElement();
        if (!resource.collection())
            element(xml, "getcontenttype", options.format().contentType());
        if (resource.div() != null) {
            element(xml, "getlastmodified", lastModified(resource));
            element(xml, "creationdate", resource.div().getTeiFile().getTimestamp().toInstant().toString());
            element(xml, "getetag", etag(resource, options));
            element(xml, "getcontentlanguage",
                    resource.div().getTeiFile().getLanguage().getISO639_1Code());
        }
        xml.writeEndElement();
        element(xml, "status", "HTTP/1.1 200 OK");
        xml.writeEndElement();
        xml.writeEndElement();
    }

    private void element(XMLStreamWriter xml, String name, String value) throws XMLStreamException {
        xml.writeStartElement("D", name, DAV_NAMESPACE);
        xml.writeCharacters(value == null ? "" : value);
        xml.writeEndElement();
    }

    private String href(DavResource resource, DavRequest davRequest, HttpServletRequest request) {
        final DavExportOptions options = davRequest.options();
        final StringBuilder href = new StringBuilder(request.getContextPath()).append(davRequest.mountPath());
        for (String segment : resource.path())
            href.append(UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8)).append('/');
        if (!resource.collection() && !resource.path().isEmpty())
            href.deleteCharAt(href.length() - 1);

        if (!davRequest.pathConfigured()) {
            href.append("?format=").append(options.format().extension())
                    .append("&fragmentation=").append(options.fragmentation());
            if (options.language() != null)
                href.append("&lang=").append(options.language().getISO639_1Code());
            if (options.author() != null)
                href.append("&author=").append(UriUtils.encodeQueryParam(options.author(), StandardCharsets.UTF_8));
        }
        return href.toString();
    }

    private DavRequest parseRequest(HttpServletRequest request) {
        final List<String> path = relativePath(request);
        if (!path.isEmpty() && "_export".equals(path.getFirst())) {
            if (path.size() < 5)
                throw new IllegalArgumentException(
                        "path-configured DAV URLs use /dav/_export/{format}/{fragmentation}/{lang|all}/{author|all}/");

            final String lang = "all".equals(path.get(3)) ? null : path.get(3);
            final String author = "all".equals(path.get(4)) ? null : path.get(4);
            final DavExportOptions options = DavExportOptions.from(path.get(1), path.get(2), lang, author);
            final String mountPath = "/dav/_export/"
                    + UriUtils.encodePathSegment(options.format().extension(), StandardCharsets.UTF_8) + "/"
                    + UriUtils.encodePathSegment(options.fragmentation(), StandardCharsets.UTF_8) + "/"
                    + (options.language() == null ? "all" : options.language().getISO639_1Code()) + "/"
                    + UriUtils.encodePathSegment(options.author() == null ? "all" : options.author(), StandardCharsets.UTF_8)
                    + "/";
            return new DavRequest(options, List.copyOf(path.subList(5, path.size())), mountPath, true);
        }

        final DavExportOptions options = DavExportOptions.from(
                request.getParameter("format"),
                request.getParameter("fragmentation"),
                request.getParameter("lang"),
                request.getParameter("author"));
        return new DavRequest(options, path, "/dav/", false);
    }

    private List<String> relativePath(HttpServletRequest request) {
        final String prefix = request.getContextPath() + "/dav";
        String path = request.getRequestURI().substring(prefix.length());
        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.endsWith("/") && !path.isEmpty())
            path = path.substring(0, path.length() - 1);
        if (path.isEmpty())
            return List.of();
        return Arrays.stream(path.split("/", -1))
                .map(it -> UriUtils.decode(it, StandardCharsets.UTF_8))
                .toList();
    }

    private String canonicalContentUri(DavResource resource) {
        return "/" + resource.path().get(1) + "/" + resource.div().getUrl();
    }

    private String etag(DavResource resource, DavExportOptions options) {
        final TeiDivIdentity identity = new TeiDivIdentity(
                resource.div().getId(), resource.div().getXpath(),
                resource.div().getTeiFile().getTimestamp().getTime());
        return "W/\"tb-" + Integer.toUnsignedString(identity.hashCode(), 16)
                + "-" + options.format().extension() + "-" + options.fragmentationDepth() + "\"";
    }

    private String lastModified(DavResource resource) {
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(
                resource.div().getTeiFile().getTimestamp().toInstant().atZone(ZoneOffset.UTC));
    }

    private record TeiDivIdentity(Long id, String xpath, long timestamp) {
    }

    private void finiteDepthError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/xml; charset=utf-8");
        response.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<D:error xmlns:D=\"DAV:\"><D:propfind-finite-depth/></D:error>");
    }
}
