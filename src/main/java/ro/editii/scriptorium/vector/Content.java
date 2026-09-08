package ro.editii.scriptorium.vector;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @AllArgsConstructor @Builder
public class Content {
    String sha256;
    String url;
    float[] embedding;
}
