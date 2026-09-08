package ro.editii.scriptorium.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
public interface EnvelopeDto {

    Dto getData();

    PagingDto getPage();

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    class Hits implements EnvelopeDto {
        HitsDto data;
        PagingDto page;
    }
}
