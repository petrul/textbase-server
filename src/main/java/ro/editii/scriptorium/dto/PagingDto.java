package ro.editii.scriptorium.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @AllArgsConstructor @Builder @JsonInclude(JsonInclude.Include.NON_NULL)
public class PagingDto {
    Integer size;
    Integer totalElements;
    Integer totalPages;
    Integer number;
}
