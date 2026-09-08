package ro.editii.scriptorium.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DivCollectionDto {
    Long id;
    String name;
    boolean isFavorites;
    Timestamp createdAt;
    List<DivCollectionItemDto> items;
}
