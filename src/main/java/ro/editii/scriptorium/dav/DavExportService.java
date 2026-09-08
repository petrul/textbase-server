package ro.editii.scriptorium.dav;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ro.editii.scriptorium.dao.TeiDivRepository;
import ro.editii.scriptorium.model.Author;
import ro.editii.scriptorium.model.Languages;
import ro.editii.scriptorium.model.TeiDiv;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DavExportService {
    private final TeiDivRepository teiDivRepository;

    public Optional<DavResource> resolve(List<String> path, DavExportOptions options) {
        final List<TeiDiv> opera = eligibleOpera(options);
        if (path.isEmpty())
            return Optional.of(new DavResource(List.of(), "Textbase", true, null));

        final Languages language = Languages.from(path.get(0));
        if (language == null || opera.stream().noneMatch(it -> languageOf(it) == language))
            return Optional.empty();
        if (path.size() == 1)
            return Optional.of(new DavResource(path, language.getISO639_1Code(), true, null));

        final String authorId = path.get(1);
        final List<TeiDiv> authorOpera = opera.stream()
                .filter(it -> languageOf(it) == language)
                .filter(it -> hasAuthor(it, authorId))
                .toList();
        if (authorOpera.isEmpty())
            return Optional.empty();
        if (path.size() == 2)
            return Optional.of(new DavResource(path, authorDisplayName(authorOpera.getFirst(), authorId), true, null));

        TeiDiv current = null;
        int divDepth = 1;
        for (int pathIndex = 2; pathIndex < path.size(); pathIndex++, divDepth++) {
            final String requestedName = path.get(pathIndex);
            final List<TeiDiv> candidates = current == null ? authorOpera : divChildren(current);
            final int currentDepth = divDepth;
            current = candidates.stream()
                    .filter(it -> entryName(it, currentDepth, options).equals(requestedName))
                    .findFirst().orElse(null);
            if (current == null)
                return Optional.empty();
            if (isFile(current, currentDepth, options) && pathIndex != path.size() - 1)
                return Optional.empty();
        }

        return Optional.of(new DavResource(
                path,
                current.getVisualLabel(),
                !isFile(current, divDepth - 1, options),
                current));
    }

    public List<DavResource> children(DavResource parent, DavExportOptions options) {
        if (!parent.collection())
            return List.of();

        final List<TeiDiv> opera = eligibleOpera(options);
        final List<String> path = parent.path();
        if (path.isEmpty()) {
            final Map<String, Languages> languages = new LinkedHashMap<>();
            opera.stream().map(this::languageOf).filter(it -> it != null)
                    .sorted(Comparator.comparing(Languages::getISO639_1Code))
                    .forEach(it -> languages.putIfAbsent(it.getISO639_1Code(), it));
            return languages.keySet().stream()
                    .map(it -> new DavResource(List.of(it), it, true, null)).toList();
        }

        final Languages language = Languages.from(path.get(0));
        if (path.size() == 1) {
            final Map<String, String> authors = new LinkedHashMap<>();
            opera.stream().filter(it -> languageOf(it) == language)
                    .flatMap(it -> it.getTeiFile().getAuthors().stream())
                    .sorted(Comparator.comparing(Author::getStrId))
                    .forEach(it -> authors.putIfAbsent(it.getStrId(), it.getVisualName()));
            return authors.entrySet().stream()
                    .map(it -> new DavResource(append(path, it.getKey()), it.getValue(), true, null)).toList();
        }

        final List<TeiDiv> divs;
        final int childDepth;
        if (path.size() == 2) {
            final String authorId = path.get(1);
            divs = opera.stream().filter(it -> languageOf(it) == language)
                    .filter(it -> hasAuthor(it, authorId)).toList();
            childDepth = 1;
        } else {
            divs = divChildren(parent.div());
            childDepth = path.size() - 1;
        }

        return divs.stream()
                .sorted()
                .map(it -> new DavResource(
                        append(path, entryName(it, childDepth, options)),
                        it.getVisualLabel(),
                        !isFile(it, childDepth, options),
                        it))
                .toList();
    }

    private List<TeiDiv> eligibleOpera(DavExportOptions options) {
        return teiDivRepository.findAllOpera().stream()
                .filter(it -> options.language() == null || languageOf(it) == options.language())
                .filter(it -> options.author() == null || hasAuthor(it, options.author()))
                .sorted()
                .toList();
    }

    private Languages languageOf(TeiDiv div) {
        return div.getTeiFile().getLanguage();
    }

    private boolean hasAuthor(TeiDiv div, String authorId) {
        return div.getTeiFile().getAuthors().stream().anyMatch(it -> it.getStrId().equals(authorId));
    }

    private String authorDisplayName(TeiDiv div, String authorId) {
        return div.getTeiFile().getAuthors().stream()
                .filter(it -> it.getStrId().equals(authorId))
                .map(Author::getVisualName)
                .findFirst().orElse(authorId);
    }

    private List<TeiDiv> divChildren(TeiDiv div) {
        if (div.getDbChildren() == null)
            return List.of();
        return div.getDbChildrenAsDivs();
    }

    private boolean isFile(TeiDiv div, int depth, DavExportOptions options) {
        return depth >= options.fragmentationDepth() || divChildren(div).isEmpty();
    }

    private String entryName(TeiDiv div, int depth, DavExportOptions options) {
        return div.getUrlFragment() + (isFile(div, depth, options) ? "." + options.format().extension() : "");
    }

    private List<String> append(List<String> path, String value) {
        final List<String> result = new ArrayList<>(path);
        result.add(value);
        return List.copyOf(result);
    }
}
