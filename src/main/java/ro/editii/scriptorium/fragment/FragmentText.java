package ro.editii.scriptorium.fragment;

import lombok.Getter;
import ro.editii.scriptorium.model.TeiDiv;

import java.util.List;

/**
 * The resolved, ready-to-render result of a Fragment: the containing div
 * (for attribution/citation - author, title, link back to source) plus the
 * selected text, one entry per paragraph/leaf spanned, already trimmed at
 * the start/end boundaries (see FragmentResolutionService). Paragraph
 * breaks between entries are a rendering concern, not this class's.
 */
@Getter
public class FragmentText {

    final TeiDiv div;
    final String start;
    final String end;
    final List<String> paragraphs;

    public FragmentText(TeiDiv div, String start, String end, List<String> paragraphs) {
        this.div = div;
        this.start = start;
        this.end = end;
        this.paragraphs = paragraphs;
    }
}
