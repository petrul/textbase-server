package ro.editii.scriptorium.tei;


import org.apache.logging.log4j.util.Strings;
import ro.editii.scriptorium.Util;
import ro.editii.scriptorium.model.TeiDiv;

import java.util.*;
import java.util.stream.Collectors;

/**
 * use this as an infinite stream of propositions of urlFragm for an TeiDiv
 */
public class CandidateUrlFragmGeneratorForTeiDivHead implements Iterable<String> {

    String head;

    public CandidateUrlFragmGeneratorForTeiDivHead(String head) {
        this.head = head;
    }

    @Override
    public Iterator<String> iterator() {
        return new CandidateUrlFragmGeneratorForTeiDivHead_Iterator(this.head);
    }
}

class CandidateUrlFragmGeneratorForTeiDivHead_Iterator implements Iterator<String> {

    // these cannot by themselves make up a url (they are somehow auxiliary)
    public static String[] PREPOZITII_SI_CONJUNCTII = {
            "a", "al", "ale", "and", "an", "à", "au", "aux",
            "care", "cand", "când", "ce", "ci", "ca", "cel", "cum", "cu",
            "comme", "comment", "ceux", "che",
            "da", "de", "din", "dar", "despre", "du", "dans", "des",
                "der", "den", "dem","das", "doi", "deux", "drei",
            "e", "et", "en", "elle", "elles", "ein", "eine",
            "for", "from", "fur", "four",
            "gli",
            "i", "iar", "însă", "insa", "in", "il", "ils", "intr",
            "între", "întru",
            "l", "lui", "le", "la", "les", "laquelle", "lequel", "las", "los",
            "mai", "ma", "mes", "mon",
            "n", "ne", "nel", "nu", "nici", "no", "non", "notre", "nos",
            "o", "ori", "on", "or", "of", "ou", "out",
            "pe", "peste", "prea", "prin", "pentru", "pro",
            "pour", "par",
            "qu", "que", "qui", "quel", "quelle",
            "sau", "sa", "si", "sub",
            "some", "sir",
            "the", "ta", "two", "trei", "trois", "three",
            "un", "una", "une", "und",
            "votre", "vos", "von", "vom",
            "zwei"
    };

    final List<String> firstPropositions = new ArrayList<>();
    final Iterator<String> firstPropositionsIt;

    // An empty head (no meaningful title text at all) has nothing for the
    // word-splitting algorithm below to grow candidates from, so it falls
    // back to a plain sequential counter (1, 2, 3, ...) instead - tracked
    // separately from "the previous candidate happened to be a number",
    // which used to be conflated with this case (see emptyHeadCounter below).
    final boolean headIsEmpty;
    int emptyHeadCounter = 1; // "1" is already served as the first proposition

    final String[] splits;
    int splits_index = 0;
    String lastAddedSplit;


    int crtEndIncrement = 2; // increment starts from 2

    // last word-composed proposition, which will be used as base for ever-increasing candidates
    String growingProposition = null;
    String lastCandidate = null;


    static Set<String> PREP_AND_CONJ_AS_LIST = new HashSet<>(Arrays.asList(PREPOZITII_SI_CONJUNCTII));

    CandidateUrlFragmGeneratorForTeiDivHead_Iterator(String head) {
        final String urlFriendify = Util.urlFriendify(head);

        this.splits = urlFriendify.split("_");
        final List<String> significantSplits = Arrays.stream(this.splits)
                .filter(it -> it.length() > 2) // except one or two-lettered words
                .filter(it -> !PREP_AND_CONJ_AS_LIST.contains(it))
                .collect(Collectors.toList());

        this.headIsEmpty = Strings.isEmpty(urlFriendify);
        if (this.headIsEmpty) {
            this.firstPropositions.add("1");
        } else if (significantSplits.size() < 4) {
            this.firstPropositions.add(urlFriendify);
        } else {
            // actually for opus urlfragm, this should be three (i.e. romanii_supt_mihai); chapters should be two.
            // String first_three_words = Arrays.asList(splits).subList(0, 2).stream().collect(Collectors.joining("_"));
            // this.firstPropositions.add(first_three_words);
        }
        this.firstPropositionsIt = this.firstPropositions.iterator();
    }

    @Override
    public boolean hasNext() {
        return true;
    }

    final Set<String> previousCandidates = new HashSet<>();

    // Leaves room for an appended "_<n>" disambiguation suffix. Without this,
    // a growingProposition already at MAX_URL_FRAGM_SIZE would have that
    // suffix truncated straight back off on every retry, returning the exact
    // same (colliding) fragment forever instead of ever actually changing.
    private static final int SUFFIX_HEADROOM = 12;

    private static String truncate(String candidate, int maxLength) {
        return candidate.length() > maxLength ? candidate.substring(0, maxLength) : candidate;
    }

    @Override
    public String next() {

        StringBuilder sb = this.growingProposition == null ?
            new StringBuilder() :
            new StringBuilder(this.growingProposition);

        // initial propositions built in the constructor are not done, serve them
        if (this.firstPropositionsIt.hasNext()) {
            final String crt = truncate(this.firstPropositionsIt.next(), TeiDiv.MAX_URL_FRAGM_SIZE);
            this.previousCandidates.add(crt);
            this.lastCandidate = crt;
            return crt;
        }

        // an empty head has no real content to grow word-based candidates
        // from - keep counting instead (see headIsEmpty above).
        if (this.headIsEmpty) {
            final String crt = String.valueOf(++this.emptyHeadCounter);
            this.previousCandidates.add(crt);
            this.lastCandidate = crt;
            return crt;
        }

        // do while the new proposed candidate has already been proposed
        // (rare but possible when the first proposal comes from the constructor and then the iterator re-builds it)
        do {
            if (this.splits_index == 0)
                this.growingProposition = null;

            boolean isAuxiliary;
            if (this.splits_index < this.splits.length) {

                // do while we only meet auxiliaries (add all auxiliaries in a bunch)
                do {
                    if (sb.length() > 0)
                        sb.append('_');

                    final String crt = splits[splits_index++];
                    isAuxiliary = this.isAuxiliary(crt);
                    sb.append(crt);
                    this.lastAddedSplit = crt;

                } while (isAuxiliary && splits_index < splits.length);

                // Untruncated here - further words may still get appended on
                // a later call, so cutting this down early would shorten the
                // growing candidate before it's actually finished growing.
                this.growingProposition = sb.toString();
            } else {
                // The head splits are terminated - including for a purely
                // numeric head, since isAuxiliary() treats digit-only
                // fragments as auxiliary too. The only way to generate a new
                // candidate is to add _1, _2, _3 etc at the end - including
                // for a numeric head: a second sibling headed "35" becomes
                // "35_2", not an unrelated "36" that would misrepresent it as
                // a genuinely different heading.
                //
                // Truncate the base with headroom reserved for the suffix
                // (SUFFIX_HEADROOM) before appending it, not after - otherwise
                // a long enough base would have the suffix truncated straight
                // back off below, returning the exact same (already-rejected,
                // colliding) fragment on every retry instead of ever actually
                // changing.
                sb = new StringBuilder(truncate(this.growingProposition, TeiDiv.MAX_URL_FRAGM_SIZE - SUFFIX_HEADROOM));
                sb.append("_" + this.crtEndIncrement++);
            }

            // Compare truncated forms: what actually gets returned/tracked
            // below is always truncated to MAX_URL_FRAGM_SIZE, so the
            // uniqueness check must use that same representation - otherwise
            // two untruncated candidates that only differ past character 100
            // would both truncate to the identical, already-emitted fragment
            // without this loop ever detecting the collision.
        } while (this.previousCandidates.contains(truncate(sb.toString(), TeiDiv.MAX_URL_FRAGM_SIZE)));

        String crtProposition = truncate(sb.toString(), TeiDiv.MAX_URL_FRAGM_SIZE);
        this.previousCandidates.add(crtProposition);
        this.lastCandidate = crtProposition;

        return crtProposition;
    }

    /**
     * auxiliary means conjuction, preposition or number
     */
    protected boolean isAuxiliary(String fragment) {
        return PREP_AND_CONJ_AS_LIST.contains(fragment)
                || fragment.matches("^\\d+$");
    }

}