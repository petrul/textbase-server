package ro.editii.scriptorium.search;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A plain literal-substring match (see GrepSearchService) - no relevance
 * ranking exists for this search mode, so score is always null (omitted
 * from the JSON response, see HitDto).
 */
@EqualsAndHashCode(callSuper = true)
public class GrepHit extends Hit {

    @Getter
    String content;

    public GrepHit(String url, String content) {
        super(url, null);
        this.content = content;
    }
}
