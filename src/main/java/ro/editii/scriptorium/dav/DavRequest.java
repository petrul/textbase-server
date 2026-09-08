package ro.editii.scriptorium.dav;

import java.util.List;

record DavRequest(
        DavExportOptions options,
        List<String> resourcePath,
        String mountPath,
        boolean pathConfigured
) {
}
