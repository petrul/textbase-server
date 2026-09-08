package ro.editii.scriptorium.fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * A "dot number notation" point within a TeiDiv's descendant tree - a
 * simpler, positional cousin of an xpath. E.g. "1.2.3" means: child 3 of
 * child 2 of child 1 of the div (1-indexed element-child navigation, same
 * addressing TeiElem.elemChild already uses for "_N" URL segments). The
 * LAST segment is not a further child index - it's a character offset into
 * the plain text of whatever element the navigation segments land on (see
 * FragmentResolutionService), matching how a real selection usually
 * bottoms out inside a paragraph's text, not on another element boundary.
 *
 * At least 2 segments are required (at least one navigation step plus the
 * offset) - a bare single number would be ambiguous (does it navigate to a
 * child, or offset into the div's own direct text? TEI divs don't
 * meaningfully have direct text of their own alongside element children),
 * so that case is rejected rather than guessed at.
 */
public final class DotPath {

    private final List<Integer> navigation;
    private final int textOffset;
    private final String raw;

    private DotPath(String raw, List<Integer> navigation, int textOffset) {
        this.raw = raw;
        this.navigation = navigation;
        this.textOffset = textOffset;
    }

    public static DotPath parse(String dotPath) {
        if (dotPath == null || dotPath.isBlank())
            throw new IllegalArgumentException("a fragment point must not be blank");

        final String[] segments = dotPath.trim().split("\\.");
        if (segments.length < 2)
            throw new IllegalArgumentException(
                    "a fragment point needs at least 2 dot-separated numbers (navigation + text offset), got: " + dotPath);

        final List<Integer> parsed = new ArrayList<>(segments.length);
        for (String segment : segments) {
            try {
                final int n = Integer.parseInt(segment.trim());
                if (n < 0)
                    throw new IllegalArgumentException("fragment point segments must not be negative: " + dotPath);
                parsed.add(n);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("not a valid dot-number fragment point: " + dotPath, e);
            }
        }

        final int textOffset = parsed.remove(parsed.size() - 1);
        // Navigation segments (everything but the trailing text offset) are
        // 1-indexed child positions (see TeiElem.elemChild) - 0 parses fine
        // above (it's a non-negative int) but is never a valid child index,
        // and would otherwise reach elemChild's onlyElements.get(nth - 1)
        // as index -1 (an ArrayIndexOutOfBoundsException, not a clean
        // "invalid fragment" error) instead of being caught here.
        for (int step : parsed) {
            if (step < 1)
                throw new IllegalArgumentException(
                        "fragment point navigation segments must be >= 1 (1-indexed), got: " + dotPath);
        }
        return new DotPath(dotPath, parsed, textOffset);
    }

    /** 1-indexed child-navigation steps, in order, from the root div down to the addressed element. */
    public List<Integer> navigation() {
        return this.navigation;
    }

    /** Character offset into the addressed element's plain text. */
    public int textOffset() {
        return this.textOffset;
    }

    public String raw() {
        return this.raw;
    }
}
