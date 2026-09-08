package ro.editii.scriptorium.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import ro.editii.scriptorium.model.DivCollectionItem;

import java.sql.Timestamp;
import java.util.List;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DivCollectionItemDto {
    Long id;
    String kind; // "DIV" or "FRAGMENT"
    String divPath;
    String divHead;
    String fragmentStart;
    String fragmentEnd;
    List<String> fragmentText; // resolved quote paragraphs - FRAGMENT items only
    Timestamp addedAt;

    public static DivCollectionItemDto from(DivCollectionItem item, List<String> resolvedFragmentText) {
        return DivCollectionItemDto.builder()
                .id(item.getId())
                .kind(item.getKind().name())
                .divPath(item.getDiv().getCompletePath())
                .divHead(item.getDiv().getHead())
                .fragmentStart(item.getFragmentStart())
                .fragmentEnd(item.getFragmentEnd())
                .fragmentText(resolvedFragmentText)
                .addedAt(item.getAddedAt())
                .build();
    }
}
