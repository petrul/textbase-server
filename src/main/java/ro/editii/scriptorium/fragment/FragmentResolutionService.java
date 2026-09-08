package ro.editii.scriptorium.fragment;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ro.editii.scriptorium.model.TeiDiv;
import ro.editii.scriptorium.model.TeiElem;
import ro.editii.scriptorium.service.ControllerTool;
import ro.editii.scriptorium.service.DivService;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves a Fragment (a div, plus a start/end pair of "dot number
 * notation" points - see DotPath) into its selected plain text: from a
 * whole subchapter down to a single character, and potentially spanning
 * several paragraphs (or sub-chapters) between a start and end that don't
 * have to land in the same one.
 *
 * Offsets are measured against each leaf element's own plain-text
 * rendering (ControllerTool.teiElemToString, the same tei2text.xsl pipeline
 * used everywhere else in this codebase) rather than the decorated HTML
 * (teidiv2html.xsl) - unambiguous character positions, at the cost of not
 * preserving inline markup (emphasis, notes, etc) in the quoted text.
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class FragmentResolutionService {

    final DivService divService;
    final ControllerTool controllerTool;

    public FragmentText resolve(TeiDiv div, String startDotPath, String endDotPath) {
        final DotPath start = DotPath.parse(startDotPath);
        final DotPath end = DotPath.parse(endDotPath);

        final TeiElem startLeaf = navigate(div, start);
        final TeiElem endLeaf = navigate(div, end);

        // getParagraphs already walks the div's whole subtree (all nested
        // sub-chapters included) in reading order - reused here rather than
        // writing a second tree-walker, at the cost of materializing the
        // whole subtree even for a short quote. Fine for Fragment's actual
        // scale (subchapter-or-smaller quotes), not meant for huge ranges.
        final List<TeiElem> leaves = this.divService.getParagraphs(div);
        final int startIdx = indexOf(leaves, startLeaf);
        final int endIdx = indexOf(leaves, endLeaf);

        if (startIdx < 0)
            throw new IllegalArgumentException(
                    "fragment start (" + start.raw() + ") doesn't land on an addressable paragraph - "
                            + "it may point at a sub-chapter boundary rather than text");
        if (endIdx < 0)
            throw new IllegalArgumentException(
                    "fragment end (" + end.raw() + ") doesn't land on an addressable paragraph - "
                            + "it may point at a sub-chapter boundary rather than text");
        if (startIdx > endIdx)
            throw new IllegalArgumentException("fragment end comes before its start in reading order");

        final List<String> paragraphs = new ArrayList<>(endIdx - startIdx + 1);
        if (startIdx == endIdx) {
            final String text = this.controllerTool.teiElemToString(leaves.get(startIdx).toElemInfo());
            paragraphs.add(trim(text, start.textOffset(), end.textOffset(), start, end));
        } else {
            for (int i = startIdx; i <= endIdx; i++) {
                String text = this.controllerTool.teiElemToString(leaves.get(i).toElemInfo());
                if (i == startIdx)
                    text = trim(text, start.textOffset(), text.length(), start, null);
                else if (i == endIdx)
                    text = trim(text, 0, end.textOffset(), null, end);
                paragraphs.add(text);
            }
        }

        // A structurally-valid range can still select zero characters (e.g.
        // start==end within one paragraph, or an end offset of 0 on a
        // multi-paragraph span's last leaf) - not useful as a "quotation",
        // so rejected here rather than silently returned as a blank/mostly-
        // blank result.
        if (paragraphs.stream().allMatch(String::isEmpty))
            throw new IllegalArgumentException(
                    "fragment " + start.raw() + ".." + end.raw() + " selects no text");

        return new FragmentText(div, start.raw(), end.raw(), paragraphs);
    }

    private TeiElem navigate(TeiDiv div, DotPath point) {
        TeiElem current = div;
        for (int step : point.navigation()) {
            try {
                current = this.divService.childElem(current, step);
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "fragment point " + point.raw() + " is invalid: " + e.getMessage(), e);
            }
        }
        return current;
    }

    private static int indexOf(List<TeiElem> leaves, TeiElem target) {
        final String targetPath = target.getCompletePath();
        for (int i = 0; i < leaves.size(); i++) {
            if (leaves.get(i).getCompletePath().equals(targetPath))
                return i;
        }
        return -1;
    }

    private static String trim(String text, int from, int to, DotPath startForError, DotPath endForError) {
        if (from < 0 || to > text.length() || from > to)
            throw new IllegalArgumentException(String.format(
                    "fragment text offset out of range (paragraph is %d chars) - start=%s end=%s",
                    text.length(),
                    startForError != null ? startForError.raw() : "n/a",
                    endForError != null ? endForError.raw() : "n/a"));
        return text.substring(from, to);
    }
}
