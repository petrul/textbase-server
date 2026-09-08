package ro.editii.scriptorium.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * sentence is a subdivision of {@link TeiPara} which is displayed in line. A simple
 * way of finding sentences of a Paragraph is to split it by dots (.)
 */
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class TeiSentence {
    int n;
    TeiDiv div;
}
