package ro.editii.scriptorium.dav;

import ro.editii.scriptorium.model.TeiDiv;

import java.util.List;

public record DavResource(
        List<String> path,
        String displayName,
        boolean collection,
        TeiDiv div
) {
}
